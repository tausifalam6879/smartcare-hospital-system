package com.smartcare.auth;

import com.smartcare.auth.service.*;
import com.smartcare.auth.repository.*;
import com.smartcare.auth.domain.StaffInvitation;
import com.smartcare.auth.web.*;
import com.smartcare.auth.web.StaffAccessDtos.*;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class StaffAccessIntegrationTest {
    @Autowired StaffAccessService staff;
    @Autowired AuthService auth;
    @Autowired HospitalService hospitals;
    @Autowired UserAccountRepository users;
    @Autowired StaffInvitationRepository invitations;
    @Autowired com.smartcare.doctor.service.DoctorService doctors;
    @Autowired com.smartcare.doctor.repository.DoctorRepository doctorRepository;
    private UUID hospital() {
        return hospitals.create(new HospitalRequest("STAFF-TEST", "Staff Hospital", "Test street", "Delhi", "Delhi",
                "110001", "+911112345678", "Asia/Kolkata", true)).id();
    }
    private RegisterRequest registration(String mobile) {
        return new RegisterRequest(mobile, null, "Test Staff", "unique-test-password", null, null, null, null, "en");
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void oneTimeMobileBoundInvitationCreatesOnlyApprovedRoles() {
        var invite = staff.issue(new IssueInvitation("+919000007001", "OFFICE_CLERK", hospital(), null));
        assertThat(invite.invitationCode()).hasSize(43);
        assertThatThrownBy(() -> staff.enroll(new StaffRegistration(invite.invitationCode(), "OFFICE_CLERK", registration("+919000007002"))))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> staff.enroll(new StaffRegistration(invite.invitationCode(), "HOSPITAL_ADMIN", registration("+919000007001"))))
                .isInstanceOf(AccessDeniedException.class);
        var enrolled = staff.enroll(new StaffRegistration(invite.invitationCode(), "OFFICE_CLERK", registration("+919000007001")));
        assertThat(enrolled.user().roles()).containsExactlyInAnyOrder("PATIENT", "RECEPTIONIST", "CASHIER");
        assertThat(auth.login(new LoginRequest("+919000007001", "unique-test-password", "OFFICE_CLERK")).accessToken()).isNotBlank();
        assertThatThrownBy(() -> auth.login(new LoginRequest("+919000007001", "unique-test-password", "HOSPITAL_ADMIN")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> staff.enroll(new StaffRegistration(invite.invitationCode(), "OFFICE_CLERK", registration("+919000007001"))))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void revokedExpiredAndUnknownCodesCannotCreateStaff() throws Exception {
        UUID hospitalId = hospital();
        var invite = staff.issue(new IssueInvitation("+919000007003", "LAB_TECHNICIAN", hospitalId, null));
        staff.revoke(invite.id());
        String expiredCode = "expired-test-code";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(expiredCode.getBytes(StandardCharsets.UTF_8)));
        invitations.save(new StaffInvitation(hash, "+919000007003", "LAB_TECHNICIAN", hospitalId, null, Instant.now().minusSeconds(60)));
        for (String code : List.of(invite.invitationCode(), expiredCode, "unknown-code")) {
            assertThatThrownBy(() -> staff.enroll(new StaffRegistration(code, "LAB_TECHNICIAN", registration("+919000007003"))))
                    .isInstanceOf(AccessDeniedException.class);
        }
        assertThat(users.existsByMobileNumber("+919000007003")).isFalse();
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007003", "SUPER_ADMIN", hospitalId, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test @WithMockUser(roles="HOSPITAL_ADMIN")
    void hospitalAdminCannotIssueUnscopedPrivileges() {
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007004", "HOSPITAL_ADMIN", UUID.randomUUID(), null)))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test
    void ordinaryPatientCannotSignInAsDoctor() {
        auth.register(registration("+919000007005"));
        assertThatThrownBy(() -> auth.login(new LoginRequest("+919000007005", "unique-test-password", "DOCTOR")))
                .isInstanceOf(BadCredentialsException.class);
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void doctorInvitationLinksOnlyTheVerifiedUnlinkedProfile() {
        UUID hospitalId = hospital();
        var department = hospitals.createDepartment(hospitalId,
                new com.smartcare.hospital.web.HospitalDtos.DepartmentRequest("STAFF-MED", "Medicine", "Staff test"));
        var doctor = doctors.create(new com.smartcare.doctor.web.DoctorDtos.DoctorRequest(hospitalId, department.id(),
                "Verified Doctor", "Medicine", "STAFF-REG-001", java.math.BigDecimal.TEN, 15, 10, "A", "1", "101", true));
        var invite = staff.issue(new IssueInvitation("+919000007006", "DOCTOR", hospitalId, doctor.id()));
        var enrolled = staff.enroll(new StaffRegistration(invite.invitationCode(), "DOCTOR", registration("+919000007006")));
        assertThat(enrolled.user().roles()).contains("DOCTOR").doesNotContain("HOSPITAL_ADMIN", "SUPER_ADMIN");
        assertThat(doctorRepository.findByLinkedUserId(enrolled.user().id()).orElseThrow().getId()).isEqualTo(doctor.id());
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007007", "DOCTOR", hospitalId, doctor.id())))
                .isInstanceOf(com.smartcare.common.error.ConflictException.class);
    }
}
