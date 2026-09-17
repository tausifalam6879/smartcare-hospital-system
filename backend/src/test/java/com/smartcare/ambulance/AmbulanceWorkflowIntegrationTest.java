package com.smartcare.ambulance;

import com.smartcare.ambulance.domain.AmbulancePriority;
import com.smartcare.ambulance.domain.AmbulanceRequestStatus;
import com.smartcare.ambulance.domain.AmbulanceStatus;
import com.smartcare.ambulance.domain.TransportType;
import com.smartcare.ambulance.service.AmbulanceService;
import com.smartcare.ambulance.web.AmbulanceDtos.AssignAmbulance;
import com.smartcare.ambulance.web.AmbulanceDtos.CancelAmbulanceRequest;
import com.smartcare.ambulance.web.AmbulanceDtos.CreateAmbulance;
import com.smartcare.ambulance.web.AmbulanceDtos.CreateAmbulanceRequest;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceLocation;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceRequestStatus;
import com.smartcare.audit.repository.AuditLogRepository;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.AuthResponse;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.common.error.ConflictException;
import com.smartcare.hospital.service.HospitalService;
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
class AmbulanceWorkflowIntegrationTest {

    @Autowired AmbulanceService ambulanceService;
    @Autowired HospitalService hospitalService;
    @Autowired AuthService auth;
    @Autowired UserAccountRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository auditLogs;

    @Test
    @WithMockUser(roles = {"PATIENT", "AMBULANCE_DISPATCHER", "HOSPITAL_ADMIN"})
    void dispatchRequiresAuthorizedAssignmentAndFollowsLockedAuditedTransitions() {
        var hospital = hospitalService.create(new HospitalRequest("SC-AMB-1", "Ambulance Workflow Hospital",
                "10 Response Road", "Delhi", "Delhi", "110001", "+911140404088", "Asia/Kolkata", true));
        AuthResponse owner = patient("71", "ambulance.owner");
        AuthResponse other = patient("72", "ambulance.other");
        AuthResponse dispatcher = patient("73", "ambulance.dispatcher");
        AuthResponse admin = patient("74", "ambulance.admin");
        users.findById(dispatcher.user().id()).orElseThrow().grantRole(Role.AMBULANCE_DISPATCHER);
        users.findById(admin.user().id()).orElseThrow().grantRole(Role.HOSPITAL_ADMIN);

        var vehicle = ambulanceService.createAmbulance(admin.user().id(), new CreateAmbulance(
                hospital.id(), "DL-TEST-AMB-01", "TEST ALPHA", "Test crew", "+919300009999",
                "Hospital bay"));
        assertThat(ambulanceService.availability(hospital.id()).availableVehicles()).isEqualTo(1);

        var created = ambulanceService.createRequest(owner.user().id(), request(hospital.id(), "amb-request-1"));
        var duplicate = ambulanceService.createRequest(owner.user().id(), request(hospital.id(), "amb-request-1"));
        assertThat(duplicate.id()).isEqualTo(created.id());
        assertThat(created.status()).isEqualTo(AmbulanceRequestStatus.REQUESTED);
        assertThat(created.ambulance()).isNull();
        assertThat(created.timeline()).singleElement()
                .satisfies(event -> assertThat(event.toStatus()).isEqualTo(AmbulanceRequestStatus.REQUESTED));
        assertThatThrownBy(() -> ambulanceService.createRequest(owner.user().id(),
                request(hospital.id(), "amb-request-new-key")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already has an active ambulance request");

        assertThatThrownBy(() -> ambulanceService.createRequest(owner.user().id(), new CreateAmbulanceRequest(
                hospital.id(), null, TransportType.BLOOD_TRANSPORT, AmbulancePriority.EMERGENCY,
                "Pickup address", null, owner.user().mobileNumber(), null, "patient-blood-transport")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> ambulanceService.cancel(other.user().id(), created.id(),
                new CancelAmbulanceRequest("Not my request"))).isInstanceOf(AccessDeniedException.class);

        var assigned = ambulanceService.assign(dispatcher.user().id(), created.id(),
                new AssignAmbulance(vehicle.id()));
        assertThat(assigned.status()).isEqualTo(AmbulanceRequestStatus.ASSIGNED);
        assertThat(assigned.ambulance().crewContact()).isEqualTo("+919300009999");
        assertThat(assigned.dispatchedAt()).isNotNull();
        assertThat(ambulanceService.availability(hospital.id()).availableVehicles()).isZero();

        var second = ambulanceService.createRequest(other.user().id(), request(hospital.id(), "amb-request-2"));
        assertThatThrownBy(() -> ambulanceService.assign(dispatcher.user().id(), second.id(),
                new AssignAmbulance(vehicle.id()))).isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> ambulanceService.updateStatus(dispatcher.user().id(), created.id(),
                new UpdateAmbulanceRequestStatus(AmbulanceRequestStatus.PATIENT_PICKED_UP, null)))
                .isInstanceOf(ConflictException.class);
        ambulanceService.acknowledge(dispatcher.user().id(), created.id());
        ambulanceService.updateStatus(dispatcher.user().id(), created.id(),
                new UpdateAmbulanceRequestStatus(AmbulanceRequestStatus.EN_ROUTE_TO_PATIENT, "Crew departed."));
        ambulanceService.updateLocation(dispatcher.user().id(), vehicle.id(), new UpdateAmbulanceLocation(
                "North Delhi response area", new BigDecimal("28.704100"), new BigDecimal("77.102500")));
        assertThat(ambulanceService.mine(owner.user().id())).singleElement().satisfies(item -> {
            assertThat(item.ambulance().currentArea()).isEqualTo("North Delhi response area");
        });

        for (AmbulanceRequestStatus status : new AmbulanceRequestStatus[]{
                AmbulanceRequestStatus.PATIENT_PICKED_UP, AmbulanceRequestStatus.EN_ROUTE_TO_HOSPITAL,
                AmbulanceRequestStatus.ARRIVED, AmbulanceRequestStatus.COMPLETED}) {
            ambulanceService.updateStatus(dispatcher.user().id(), created.id(),
                    new UpdateAmbulanceRequestStatus(status, null));
        }
        var completed = ambulanceService.mine(owner.user().id()).get(0);
        assertThat(completed.status()).isEqualTo(AmbulanceRequestStatus.COMPLETED);
        assertThat(completed.timeline()).hasSize(8);
        assertThat(ambulanceService.availability(hospital.id()).availableVehicles()).isEqualTo(1);

        var cancelled = ambulanceService.cancel(other.user().id(), second.id(),
                new CancelAmbulanceRequest("Transport no longer required."));
        assertThat(cancelled.status()).isEqualTo(AmbulanceRequestStatus.CANCELLED);
        assertThat(notifications.findAll()).extracting(item -> item.getType())
                .contains(NotificationType.AMBULANCE_REQUEST_CREATED, NotificationType.AMBULANCE_REQUEST_UPDATED);
        assertThat(auditLogs.count()).isGreaterThanOrEqualTo(12);
    }

    private CreateAmbulanceRequest request(java.util.UUID hospitalId, String idempotencyKey) {
        return new CreateAmbulanceRequest(hospitalId, null, TransportType.PATIENT_TRANSPORT,
                AmbulancePriority.EMERGENCY, "42 Test Colony, Delhi 110001", "Blue gate",
                "+919300000071", "Wheelchair access requested.", idempotencyKey);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9193000000" + digits, label + "@example.com",
                "Ambulance Workflow User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
