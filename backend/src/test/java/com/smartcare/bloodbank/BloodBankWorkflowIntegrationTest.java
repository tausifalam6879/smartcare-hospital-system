package com.smartcare.bloodbank;

import com.smartcare.audit.repository.AuditLogRepository;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.AuthResponse;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.bloodbank.domain.AvailabilityStatus;
import com.smartcare.bloodbank.domain.BloodBankSourceType;
import com.smartcare.bloodbank.domain.BloodComponent;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodbank.domain.BloodRequestStatus;
import com.smartcare.bloodbank.domain.BloodUrgency;
import com.smartcare.bloodbank.domain.DonorEligibilityStatus;
import com.smartcare.bloodbank.domain.InventoryVerificationStatus;
import com.smartcare.bloodbank.service.BloodBankService;
import com.smartcare.bloodbank.web.BloodBankDtos.BloodBankRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.CancelBloodRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.CreateBloodRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.DonorConsentRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.DonorVerificationRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.InventoryRequest;
import com.smartcare.doctor.service.DoctorService;
import com.smartcare.doctor.web.DoctorDtos.DoctorRequest;
import com.smartcare.doctor.web.DoctorDtos.LinkAccountRequest;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BloodBankWorkflowIntegrationTest {

    @Autowired BloodBankService bloodBank;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;
    @Autowired AuthService auth;
    @Autowired UserAccountRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository auditLogs;

    @Test
    @WithMockUser(roles = {"HOSPITAL_ADMIN", "BLOOD_BANK_STAFF", "DOCTOR"})
    void verifiedInventoryIsReservedSafelyAndVisibleOnlyThroughAuthorizedWorkflow() {
        var hospital = hospitals.create(new HospitalRequest("SC-BLOOD-1", "Blood Workflow Hospital",
                "9 Safety Avenue", "Delhi", "Delhi", "110001", "+911112345699", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("EMER", "Emergency Medicine", "Emergency blood support"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Blood Safety",
                "Emergency Medicine", "SC-BLOOD-REG-1", new BigDecimal("800.00"), 15, 20,
                "Emergency Block", "Ground Floor", "ER 1", true));

        AuthResponse patientOne = patient("61", "blood.owner");
        AuthResponse patientTwo = patient("62", "blood.other");
        AuthResponse doctorAccount = patient("63", "blood.doctor");
        AuthResponse staffAccount = patient("64", "blood.staff");
        AuthResponse donorAccount = patient("65", "blood.donor");
        doctors.linkAccount(doctor.id(), new LinkAccountRequest(doctorAccount.user().id()));
        users.findById(staffAccount.user().id()).orElseThrow().grantRole(Role.BLOOD_BANK_STAFF);

        var bank = bloodBank.createBank(new BloodBankRequest(hospital.id(), "SC-BB-01",
                "City General Hospital Blood Centre", "Emergency Block, Ground Floor", "+911140404099",
                new BigDecimal("0.00"), 0, BloodBankSourceType.HOSPITAL_MANAGED));
        bloodBank.verifyInventory(staffAccount.user().id(), new InventoryRequest(bank.id(),
                BloodGroup.O_NEGATIVE, BloodComponent.PACKED_RED_CELLS, "ON-260822-A",
                LocalDate.now().plusDays(21), 4, InventoryVerificationStatus.VERIFIED));

        assertThat(bloodBank.availability(hospital.id(), BloodGroup.O_NEGATIVE,
                BloodComponent.PACKED_RED_CELLS, 3)).singleElement().satisfies(item -> {
                    assertThat(item.status()).isEqualTo(AvailabilityStatus.AVAILABLE);
                    assertThat(item.availableUnits()).isEqualTo(4);
                    assertThat(item.lastVerifiedAt()).isNotNull();
                });

        assertThatThrownBy(() -> bloodBank.createRequest(patientOne.user().id(), request(
                patientOne.user().patientNumber(), hospital.id(), 1, "patient-not-authorized")))
                .isInstanceOf(AccessDeniedException.class);

        var first = bloodBank.createRequest(doctorAccount.user().id(), request(
                patientOne.user().patientNumber(), hospital.id(), 3, "blood-request-1"));
        var duplicate = bloodBank.createRequest(doctorAccount.user().id(), request(
                patientOne.user().patientNumber(), hospital.id(), 3, "blood-request-1"));
        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(first.status()).isEqualTo(BloodRequestStatus.RESERVED);
        assertThat(first.matchedUnits()).isEqualTo(3);
        assertThat(first.allocations()).singleElement().satisfies(item -> assertThat(item.units()).isEqualTo(3));

        var second = bloodBank.createRequest(doctorAccount.user().id(), request(
                patientTwo.user().patientNumber(), hospital.id(), 2, "blood-request-2"));
        assertThat(second.status()).isEqualTo(BloodRequestStatus.PARTIALLY_RESERVED);
        assertThat(second.matchedUnits()).isEqualTo(1);
        assertThat(bloodBank.mine(patientOne.user().id())).extracting(item -> item.id()).containsExactly(first.id());
        assertThat(bloodBank.mine(patientTwo.user().id())).extracting(item -> item.id()).containsExactly(second.id());

        var donor = bloodBank.consent(donorAccount.user().id(), new DonorConsentRequest("MOBILE"));
        bloodBank.verifyDonor(staffAccount.user().id(), donor.id(),
                new DonorVerificationRequest(BloodGroup.O_NEGATIVE, DonorEligibilityStatus.ELIGIBLE));
        assertThat(bloodBank.donorMatches(staffAccount.user().id(), second.id())).singleElement()
                .satisfies(match -> assertThat(match.mobileNumber()).isEqualTo(donorAccount.user().mobileNumber()));

        bloodBank.cancel(doctorAccount.user().id(), second.id(), new CancelBloodRequest("Alternative arranged"));
        var fulfilled = bloodBank.fulfil(staffAccount.user().id(), first.id());
        assertThat(fulfilled.status()).isEqualTo(BloodRequestStatus.FULFILLED);
        assertThat(bloodBank.availability(hospital.id(), BloodGroup.O_NEGATIVE,
                BloodComponent.PACKED_RED_CELLS, 1)).singleElement()
                .satisfies(item -> assertThat(item.availableUnits()).isEqualTo(1));

        assertThat(notifications.findAll()).extracting(item -> item.getType())
                .contains(NotificationType.BLOOD_REQUEST_CREATED, NotificationType.BLOOD_REQUEST_UPDATED);
        assertThat(auditLogs.count()).isGreaterThanOrEqualTo(14);
    }

    private CreateBloodRequest request(String patientNumber, java.util.UUID hospitalId, int units,
                                       String idempotencyKey) {
        return new CreateBloodRequest(patientNumber, hospitalId, null, BloodGroup.O_NEGATIVE,
                BloodComponent.PACKED_RED_CELLS, units, BloodUrgency.EMERGENCY,
                "Clinician-authorized emergency transfusion support.", idempotencyKey);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9193000000" + digits, label + "@example.com",
                "Blood Workflow User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
