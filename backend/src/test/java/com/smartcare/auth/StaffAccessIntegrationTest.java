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
    @Autowired StaffAccessRequestService requests;
    @Autowired StaffAccessRequestRepository requestRepository;
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
        assertThat(staff.invitableDoctors(hospitalId)).isEmpty();
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007007", "DOCTOR", hospitalId, doctor.id())))
                .isInstanceOf(com.smartcare.common.error.ConflictException.class);
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void requestReviewInvitationActivationAndPasswordLoginFormOneConnectedFlow() {
        UUID hospitalId = hospital();
        var input = new RequestStaffAccess("Test Clerk", "+919000007020", "clerk@example.test", "OFFICE_CLERK", hospitalId, "Desk reference");
        requests.submit(input);
        requests.submit(input);
        var pending = requests.pending(hospitalId, 0);
        assertThat(pending.totalElements()).isEqualTo(1);
        assertThat(users.existsByMobileNumber(input.mobileNumber())).isFalse();
        UUID requestId = pending.content().get(0).id();
        var invitation = staff.issue(new IssueInvitation(input.mobileNumber(), "OFFICE_CLERK", hospitalId, null, requestId));
        assertThat(requestRepository.findById(requestId).orElseThrow().getStatus()).isEqualTo("APPROVED");
        assertThat(requests.pending(hospitalId, 0).content()).isEmpty();
        var summary = staff.invitations(hospitalId, 0).content().get(0);
        assertThat(summary.status()).isEqualTo("ACTIVE");
        assertThat(summary.mobileNumber()).isEqualTo(input.mobileNumber());
        assertThat(summary.toString()).doesNotContain(invitation.invitationCode());
        staff.enroll(new StaffRegistration(invitation.invitationCode(), "OFFICE_CLERK", registration(input.mobileNumber())));
        assertThat(staff.invitations(hospitalId, 0).content().get(0).status()).isEqualTo("ACCEPTED");
        assertThat(auth.login(new LoginRequest(input.mobileNumber(), "unique-test-password", "OFFICE_CLERK")).user().roles())
                .contains("RECEPTIONIST", "CASHIER");
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void activeInvitationsMustBeRevokedBeforeReplacementAndConsumedInvitationsCannotBeRevoked() {
        UUID hospitalId = hospital();
        var first = staff.issue(new IssueInvitation("+919000007021", "CASHIER", hospitalId, null));
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007021", "CASHIER", hospitalId, null)))
                .isInstanceOf(com.smartcare.common.error.ConflictException.class);
        staff.revoke(first.id());
        var replacement = staff.issue(new IssueInvitation("+919000007021", "CASHIER", hospitalId, null));
        assertThat(replacement.invitationCode()).isNotEqualTo(first.invitationCode());
        assertThatThrownBy(() -> staff.enroll(new StaffRegistration(first.invitationCode(), "CASHIER", registration("+919000007021"))))
                .isInstanceOf(AccessDeniedException.class);
        staff.enroll(new StaffRegistration(replacement.invitationCode(), "CASHIER", registration("+919000007021")));
        assertThatThrownBy(() -> staff.revoke(replacement.id())).isInstanceOf(com.smartcare.common.error.ConflictException.class);
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void requestCannotBeApprovedForAnotherRoleAndRejectedRequestsGrantNoAccess() {
        UUID hospitalId = hospital();
        requests.submit(new RequestStaffAccess("Test Staff", "+919000007022", null, "CASHIER", hospitalId, "Desk"));
        UUID requestId = requests.pending(hospitalId, 0).content().get(0).id();
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007022", "HOSPITAL_ADMIN", hospitalId, null, requestId)))
                .isInstanceOf(com.smartcare.common.error.ConflictException.class);
        requests.reject(requestId);
        assertThat(requestRepository.findById(requestId).orElseThrow().getStatus()).isEqualTo("REJECTED");
        assertThat(users.existsByMobileNumber("+919000007022")).isFalse();
        assertThatThrownBy(() -> staff.issue(new IssueInvitation("+919000007022", "CASHIER", hospitalId, null, requestId)))
                .isInstanceOf(com.smartcare.common.error.ConflictException.class);
    }
    @Test @WithMockUser(roles="PATIENT")
    void patientCannotReadRequestsOrInvitationsOrRejectRequests() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> staff.invitations(id, 0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> staff.invitableDoctors(id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requests.pending(id, 0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requests.reject(id)).isInstanceOf(AccessDeniedException.class);
    }
    @Test @WithMockUser(roles="SUPER_ADMIN")
    void existingPatientNeedsTheCurrentPasswordAndKeepsTheirAccountAndPatientProfile() {
        UUID hospitalId = hospital();
        var patient = auth.register(registration("+919000007023"));
        var invitation = staff.issue(new IssueInvitation(patient.user().mobileNumber(), "OFFICE_CLERK", hospitalId, null));
        assertThatThrownBy(() -> staff.activateExisting(new ExistingStaffActivation(invitation.invitationCode(),
                "OFFICE_CLERK", patient.user().mobileNumber(), "wrong-password"))).isInstanceOf(BadCredentialsException.class);
        assertThat(users.findById(patient.user().id()).orElseThrow().getRoles()).containsExactly(com.smartcare.auth.domain.Role.PATIENT);
        assertThat(invitations.findById(invitation.id()).orElseThrow().usable(Instant.now())).isTrue();
        var activated = staff.activateExisting(new ExistingStaffActivation(invitation.invitationCode(),
                "OFFICE_CLERK", patient.user().mobileNumber(), "unique-test-password"));
        assertThat(activated.user().id()).isEqualTo(patient.user().id());
        assertThat(activated.user().roles()).contains("PATIENT", "RECEPTIONIST", "CASHIER");
        assertThat(auth.login(new LoginRequest(patient.user().mobileNumber(), "unique-test-password", "OFFICE_CLERK")).user().id())
                .isEqualTo(patient.user().id());
    }
}
