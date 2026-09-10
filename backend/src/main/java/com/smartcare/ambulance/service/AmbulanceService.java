package com.smartcare.ambulance.service;

import com.smartcare.ambulance.domain.Ambulance;
import com.smartcare.ambulance.domain.AmbulanceRequest;
import com.smartcare.ambulance.domain.AmbulanceRequestEvent;
import com.smartcare.ambulance.domain.AmbulanceRequestStatus;
import com.smartcare.ambulance.domain.AmbulanceStatus;
import com.smartcare.ambulance.domain.TransportType;
import com.smartcare.ambulance.repository.AmbulanceRepository;
import com.smartcare.ambulance.repository.AmbulanceRequestEventRepository;
import com.smartcare.ambulance.repository.AmbulanceRequestRepository;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceAvailabilityResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceEventResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceRequestResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AssignAmbulance;
import com.smartcare.ambulance.web.AmbulanceDtos.AssignedAmbulanceResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.CancelAmbulanceRequest;
import com.smartcare.ambulance.web.AmbulanceDtos.CreateAmbulance;
import com.smartcare.ambulance.web.AmbulanceDtos.CreateAmbulanceRequest;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceAvailability;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceLocation;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceRequestStatus;
import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AmbulanceService {
    private static final Set<Role> DISPATCH_ROLES = Set.of(
            Role.AMBULANCE_DISPATCHER, Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);
    private static final Set<Role> OPERATIONAL_REQUEST_ROLES = Set.of(
            Role.DOCTOR, Role.RECEPTIONIST, Role.BLOOD_BANK_STAFF,
            Role.AMBULANCE_DISPATCHER, Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);

    private final AmbulanceRepository ambulances;
    private final AmbulanceRequestRepository requests;
    private final AmbulanceRequestEventRepository events;
    private final HospitalRepository hospitals;
    private final PatientRepository patients;
    private final UserAccountRepository users;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public AmbulanceService(AmbulanceRepository ambulances, AmbulanceRequestRepository requests,
                            AmbulanceRequestEventRepository events, HospitalRepository hospitals,
                            PatientRepository patients, UserAccountRepository users,
                            NotificationService notifications, AuditService audit, Clock clock) {
        this.ambulances = ambulances;
        this.requests = requests;
        this.events = events;
        this.hospitals = hospitals;
        this.patients = patients;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AmbulanceAvailabilityResponse availability(UUID hospitalId) {
        Hospital hospital = requireHospital(hospitalId);
        List<Ambulance> fleet = ambulances.findAllByHospitalIdAndActiveTrueOrderByCallSignAsc(hospitalId);
        long available = fleet.stream().filter(item -> item.getStatus() == AmbulanceStatus.AVAILABLE).count();
        boolean synthetic = !fleet.isEmpty() && fleet.stream().allMatch(Ambulance::isSynthetic);
        return new AmbulanceAvailabilityResponse(hospitalId, hospital.getName(), available, fleet.size(),
                synthetic, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<AmbulanceResponse> fleet(UUID userId, UUID hospitalId) {
        requireDispatcher(userId);
        requireHospital(hospitalId);
        audit.record("AMBULANCE_FLEET_VIEWED", "HOSPITAL", hospitalId, hospitalId);
        return ambulances.findAllByHospitalIdAndActiveTrueOrderByCallSignAsc(hospitalId).stream()
                .map(AmbulanceService::toResponse).toList();
    }

    @Transactional
    public AmbulanceResponse createAmbulance(UUID userId, CreateAmbulance input) {
        requireAdmin(userId);
        Hospital hospital = requireHospital(input.hospitalId());
        String registration = normalizeUpper(input.registrationNumber());
        String callSign = normalizeUpper(input.callSign());
        if (ambulances.existsByRegistrationNumberIgnoreCase(registration)) {
            throw new ConflictException("An ambulance already uses this registration number.");
        }
        if (ambulances.existsByHospitalIdAndCallSignIgnoreCase(hospital.getId(), callSign)) {
            throw new ConflictException("An ambulance already uses this call sign at the hospital.");
        }
        Ambulance ambulance = ambulances.save(new Ambulance(hospital, registration, callSign,
                input.crewLabel().trim(), blankToNull(input.crewContact()), blankToNull(input.currentArea()), false));
        audit.record("AMBULANCE_CREATED", "AMBULANCE", ambulance.getId(), hospital.getId());
        return toResponse(ambulance);
    }

    @Transactional
    public AmbulanceResponse updateLocation(UUID userId, UUID ambulanceId, UpdateAmbulanceLocation input) {
        requireDispatcher(userId);
        Ambulance ambulance = lockAmbulance(ambulanceId);
        ambulance.updateLocation(input.currentArea().trim(), input.latitude(), input.longitude(), clock.instant());
        audit.record("AMBULANCE_LOCATION_UPDATED", "AMBULANCE", ambulance.getId(),
                ambulance.getHospital().getId());
        return toResponse(ambulance);
    }

    @Transactional
    public AmbulanceResponse updateAvailability(UUID userId, UUID ambulanceId,
                                                UpdateAmbulanceAvailability input) {
        requireDispatcher(userId);
        Ambulance ambulance = lockAmbulance(ambulanceId);
        if (input.status() != AmbulanceStatus.AVAILABLE && input.status() != AmbulanceStatus.OUT_OF_SERVICE) {
            throw new IllegalArgumentException("Only AVAILABLE or OUT_OF_SERVICE is accepted here.");
        }
        transition(() -> {
            if (input.status() == AmbulanceStatus.OUT_OF_SERVICE) ambulance.markOutOfService();
            else ambulance.returnToService();
        });
        audit.record("AMBULANCE_AVAILABILITY_UPDATED", "AMBULANCE", ambulance.getId(),
                ambulance.getHospital().getId());
        return toResponse(ambulance);
    }

    @Transactional
    public AmbulanceRequestResponse createRequest(UUID userId, CreateAmbulanceRequest input) {
        UserAccount requester = requireUser(userId);
        String idempotencyKey = input.idempotencyKey().trim();
        var existing = requests.findByRequestedByIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) return toRequestResponse(existing.get());

        Hospital hospital = requireHospital(input.hospitalId());
        boolean dispatcherOrStaff = hasAnyRole(requester, OPERATIONAL_REQUEST_ROLES);
        Patient patient;
        if (dispatcherOrStaff) {
            patient = blankToNull(input.patientNumber()) == null ? null
                    : patients.findByPatientNumberIgnoreCase(input.patientNumber().trim())
                    .orElseThrow(() -> new NotFoundException("Patient was not found."));
        } else {
            if (!requester.getRoles().contains(Role.PATIENT)) throw new AccessDeniedException("Patient role required.");
            if (input.transportType() != TransportType.PATIENT_TRANSPORT) {
                throw new AccessDeniedException("Patients can request patient transport only.");
            }
            patient = requirePatient(userId);
        }
        if (input.transportType() == TransportType.BLOOD_TRANSPORT && !dispatcherOrStaff) {
            throw new AccessDeniedException("Blood transport requires authorized operational staff.");
        }

        Instant now = clock.instant();
        AmbulanceRequest request = requests.save(new AmbulanceRequest(patient, hospital, requester,
                input.transportType(), input.priority(), input.pickupAddress().trim(),
                blankToNull(input.pickupLandmark()), input.contactNumber().trim(),
                blankToNull(input.assistanceNotes()), idempotencyKey, now));
        events.save(new AmbulanceRequestEvent(request, requester, null, AmbulanceRequestStatus.REQUESTED,
                "Request recorded; dispatch approval is pending.", now));
        audit.record("AMBULANCE_REQUEST_CREATED", "AMBULANCE_REQUEST", request.getId(), hospital.getId());
        notifyPatient(request, NotificationType.AMBULANCE_REQUEST_CREATED, "created",
                "Ambulance request received", "Your request is waiting for an authorized dispatcher. No vehicle has been assigned yet.");
        return toRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public List<AmbulanceRequestResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        return requests.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::toRequestResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AmbulanceRequestResponse> worklist(UUID userId, UUID hospitalId, AmbulanceRequestStatus status) {
        requireDispatcher(userId);
        requireHospital(hospitalId);
        audit.record("AMBULANCE_WORKLIST_VIEWED", "HOSPITAL", hospitalId, hospitalId);
        List<AmbulanceRequest> result = status == null
                ? requests.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId)
                : requests.findAllByHospitalIdAndStatusOrderByCreatedAtDesc(hospitalId, status);
        return result.stream().map(this::toRequestResponse).toList();
    }

    @Transactional
    public AmbulanceRequestResponse assign(UUID userId, UUID requestId, AssignAmbulance input) {
        UserAccount dispatcher = requireDispatcher(userId);
        AmbulanceRequest request = lockRequest(requestId);
        Ambulance ambulance = lockAmbulance(input.ambulanceId());
        if (!request.getHospital().getId().equals(ambulance.getHospital().getId())) {
            throw new ConflictException("The ambulance belongs to a different hospital.");
        }
        if (!ambulance.isActive()) throw new ConflictException("The ambulance is inactive.");
        Instant now = clock.instant();
        transition(() -> {
            ambulance.assign();
            request.assign(ambulance, now);
        });
        event(request, dispatcher, AmbulanceRequestStatus.REQUESTED, AmbulanceRequestStatus.ASSIGNED,
                "Vehicle assigned; crew acknowledgement is pending.", now);
        audit.record("AMBULANCE_ASSIGNED", "AMBULANCE_REQUEST", request.getId(), request.getHospital().getId());
        notifyPatient(request, NotificationType.AMBULANCE_REQUEST_UPDATED, "assigned",
                "Ambulance assigned", "A dispatcher has assigned a vehicle. Crew acknowledgement is still pending.");
        return toRequestResponse(request);
    }

    @Transactional
    public AmbulanceRequestResponse acknowledge(UUID userId, UUID requestId) {
        UserAccount dispatcher = requireDispatcher(userId);
        AmbulanceRequest request = lockRequest(requestId);
        Instant now = clock.instant();
        transition(() -> request.acknowledge(now));
        event(request, dispatcher, AmbulanceRequestStatus.ASSIGNED, AmbulanceRequestStatus.ACKNOWLEDGED,
                "Assigned crew acknowledged the dispatch.", now);
        audit.record("AMBULANCE_DISPATCH_ACKNOWLEDGED", "AMBULANCE_REQUEST", request.getId(),
                request.getHospital().getId());
        notifyPatient(request, NotificationType.AMBULANCE_REQUEST_UPDATED, "acknowledged",
                "Dispatch acknowledged", "The assigned ambulance crew has acknowledged the request.");
        return toRequestResponse(request);
    }

    @Transactional
    public AmbulanceRequestResponse updateStatus(UUID userId, UUID requestId,
                                                 UpdateAmbulanceRequestStatus input) {
        UserAccount dispatcher = requireDispatcher(userId);
        AmbulanceRequest request = lockRequest(requestId);
        if (request.getAmbulance() == null) throw new ConflictException("No ambulance is assigned.");
        Ambulance ambulance = lockAmbulance(request.getAmbulance().getId());
        AmbulanceRequestStatus from = request.getStatus();
        Instant now = clock.instant();
        transition(() -> request.advance(input.status(), now));
        if (input.status() == AmbulanceRequestStatus.COMPLETED) ambulance.release();
        else ambulance.operationalStatus(toAmbulanceStatus(input.status()));
        event(request, dispatcher, from, input.status(), blankToNull(input.note()), now);
        audit.record("AMBULANCE_REQUEST_STATUS_UPDATED", "AMBULANCE_REQUEST", request.getId(),
                request.getHospital().getId());
        notifyPatient(request, NotificationType.AMBULANCE_REQUEST_UPDATED,
                "status:" + input.status(), "Ambulance status updated", statusMessage(input.status()));
        return toRequestResponse(request);
    }

    @Transactional
    public AmbulanceRequestResponse cancel(UUID userId, UUID requestId, CancelAmbulanceRequest input) {
        UserAccount actor = requireUser(userId);
        AmbulanceRequest request = lockRequest(requestId);
        boolean dispatcher = hasAnyRole(actor, DISPATCH_ROLES);
        boolean owner = request.getPatient() != null && request.getPatient().getUser().getId().equals(userId);
        boolean requester = request.getRequestedBy().getId().equals(userId);
        if (!dispatcher && !owner && !requester) throw new AccessDeniedException("This request does not belong to you.");
        if (!dispatcher && request.getStatus() != AmbulanceRequestStatus.REQUESTED) {
            throw new ConflictException("After dispatch starts, contact the hospital desk to cancel safely.");
        }
        AmbulanceRequestStatus from = request.getStatus();
        Instant now = clock.instant();
        transition(() -> request.cancel(now, input.reason().trim()));
        if (request.getAmbulance() != null) lockAmbulance(request.getAmbulance().getId()).release();
        event(request, actor, from, AmbulanceRequestStatus.CANCELLED, input.reason().trim(), now);
        audit.record("AMBULANCE_REQUEST_CANCELLED", "AMBULANCE_REQUEST", request.getId(),
                request.getHospital().getId());
        notifyPatient(request, NotificationType.AMBULANCE_REQUEST_UPDATED, "cancelled",
                "Ambulance request cancelled", "The transport request has been cancelled.");
        return toRequestResponse(request);
    }

    private AmbulanceRequestResponse toRequestResponse(AmbulanceRequest request) {
        Patient patient = request.getPatient();
        Hospital hospital = request.getHospital();
        Ambulance ambulance = request.getAmbulance();
        AssignedAmbulanceResponse assigned = ambulance == null ? null : new AssignedAmbulanceResponse(
                ambulance.getId(), ambulance.getRegistrationNumber(), ambulance.getCallSign(),
                ambulance.getStatus(), ambulance.getCurrentArea(), ambulance.getLocationUpdatedAt(),
                ambulance.isSynthetic());
        List<AmbulanceEventResponse> timeline = events.findAllByRequestIdOrderByEventAtAsc(request.getId()).stream()
                .map(item -> new AmbulanceEventResponse(item.getFromStatus(), item.getToStatus(),
                        item.getActor().getId().equals(request.getRequestedBy().getId()) ? "Requester" : "Dispatch team",
                        item.getNote(), item.getEventAt())).toList();
        return new AmbulanceRequestResponse(request.getId(), patient == null ? null : patient.getPatientNumber(),
                hospital.getId(), hospital.getName(), hospital.getAddressLine(), hospital.getContactNumber(),
                request.getTransportType(), request.getPriority(), request.getStatus(), request.getPickupAddress(),
                request.getPickupLandmark(), request.getContactNumber(), request.getAssistanceNotes(), assigned,
                request.getStatusUpdatedAt(), request.getDispatchedAt(), request.getAcknowledgedAt(),
                request.getCompletedAt(), request.getCancelledAt(), request.getCancellationReason(),
                request.getCreatedAt(), timeline);
    }

    private void event(AmbulanceRequest request, UserAccount actor, AmbulanceRequestStatus from,
                       AmbulanceRequestStatus to, String note, Instant at) {
        events.save(new AmbulanceRequestEvent(request, actor, from, to, note, at));
    }

    private void notifyPatient(AmbulanceRequest request, NotificationType type, String eventKey,
                               String title, String message) {
        if (request.getPatient() != null) notifications.notifyPatient(request.getPatient(),
                request.getHospital().getId(), type, request.getId(), eventKey, title, message);
    }

    private static AmbulanceStatus toAmbulanceStatus(AmbulanceRequestStatus status) {
        return switch (status) {
            case EN_ROUTE_TO_PATIENT -> AmbulanceStatus.EN_ROUTE_TO_PATIENT;
            case PATIENT_PICKED_UP -> AmbulanceStatus.PATIENT_PICKED_UP;
            case EN_ROUTE_TO_HOSPITAL -> AmbulanceStatus.EN_ROUTE_TO_HOSPITAL;
            case ARRIVED -> AmbulanceStatus.ARRIVED;
            default -> throw new IllegalArgumentException("This status is not an operational vehicle stage.");
        };
    }

    private static String statusMessage(AmbulanceRequestStatus status) {
        return switch (status) {
            case EN_ROUTE_TO_PATIENT -> "The ambulance is reported en route to the pickup location.";
            case PATIENT_PICKED_UP -> "The dispatcher has recorded that the patient was picked up.";
            case EN_ROUTE_TO_HOSPITAL -> "The ambulance is reported en route to the destination hospital.";
            case ARRIVED -> "The dispatcher has recorded arrival at the hospital.";
            case COMPLETED -> "The transport workflow has been completed.";
            default -> "The ambulance request status has changed.";
        };
    }

    private static AmbulanceResponse toResponse(Ambulance item) {
        return new AmbulanceResponse(item.getId(), item.getHospital().getId(), item.getHospital().getName(),
                item.getRegistrationNumber(), item.getCallSign(), item.getStatus(), item.getCrewLabel(),
                item.getCrewContact(), item.getCurrentArea(), item.getLatitude(), item.getLongitude(),
                item.getLocationUpdatedAt(), item.isActive(), item.isSynthetic());
    }

    private AmbulanceRequest lockRequest(UUID id) {
        return requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Ambulance request was not found."));
    }

    private Ambulance lockAmbulance(UUID id) {
        return ambulances.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Ambulance was not found."));
    }

    private Hospital requireHospital(UUID id) {
        Hospital hospital = hospitals.findById(id).orElseThrow(() -> new NotFoundException("Hospital was not found."));
        if (!hospital.isActive()) throw new ConflictException("Hospital is inactive.");
        return hospital;
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }

    private UserAccount requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new AccessDeniedException("Account was not found."));
    }

    private UserAccount requireDispatcher(UUID id) {
        UserAccount user = requireUser(id);
        if (!hasAnyRole(user, DISPATCH_ROLES)) throw new AccessDeniedException("Dispatcher role required.");
        return user;
    }

    private UserAccount requireAdmin(UUID id) {
        UserAccount user = requireUser(id);
        if (!hasAnyRole(user, Set.of(Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN))) {
            throw new AccessDeniedException("Administrator role required.");
        }
        return user;
    }

    private static boolean hasAnyRole(UserAccount user, Set<Role> roles) {
        return user.getRoles().stream().anyMatch(roles::contains);
    }

    private static String normalizeUpper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void transition(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }
}
