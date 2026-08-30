package com.raahmediq.bloodbank.service;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.appointment.repository.AppointmentRepository;
import com.raahmediq.audit.service.AuditService;
import com.raahmediq.auth.domain.Role;
import com.raahmediq.auth.domain.UserAccount;
import com.raahmediq.auth.repository.UserAccountRepository;
import com.raahmediq.bloodbank.config.BloodBankProperties;
import com.raahmediq.bloodbank.domain.AvailabilityStatus;
import com.raahmediq.bloodbank.domain.BloodAllocation;
import com.raahmediq.bloodbank.domain.BloodAllocationStatus;
import com.raahmediq.bloodbank.domain.BloodBank;
import com.raahmediq.bloodbank.domain.BloodComponent;
import com.raahmediq.bloodbank.domain.BloodGroup;
import com.raahmediq.bloodbank.domain.BloodInventoryBatch;
import com.raahmediq.bloodbank.domain.BloodRequest;
import com.raahmediq.bloodbank.domain.BloodRequestStatus;
import com.raahmediq.bloodbank.domain.DonorEligibilityStatus;
import com.raahmediq.bloodbank.domain.DonorOptIn;
import com.raahmediq.bloodbank.domain.InventoryVerificationStatus;
import com.raahmediq.bloodbank.repository.BloodAllocationRepository;
import com.raahmediq.bloodbank.repository.BloodBankRepository;
import com.raahmediq.bloodbank.repository.BloodInventoryBatchRepository;
import com.raahmediq.bloodbank.repository.BloodRequestRepository;
import com.raahmediq.bloodbank.repository.DonorOptInRepository;
import com.raahmediq.bloodbank.web.BloodBankDtos.AllocationResponse;
import com.raahmediq.bloodbank.web.BloodBankDtos.AvailabilityResponse;
import com.raahmediq.bloodbank.web.BloodBankDtos.BloodBankRequest;
import com.raahmediq.bloodbank.web.BloodBankDtos.BloodBankResponse;
import com.raahmediq.bloodbank.web.BloodBankDtos.BloodRequestResponse;
import com.raahmediq.bloodbank.web.BloodBankDtos.CancelBloodRequest;
import com.raahmediq.bloodbank.web.BloodBankDtos.CreateBloodRequest;
import com.raahmediq.bloodbank.web.BloodBankDtos.DonorConsentRequest;
import com.raahmediq.bloodbank.web.BloodBankDtos.DonorMatchResponse;
import com.raahmediq.bloodbank.web.BloodBankDtos.DonorResponse;
import com.raahmediq.bloodbank.web.BloodBankDtos.DonorVerificationRequest;
import com.raahmediq.bloodbank.web.BloodBankDtos.InventoryRequest;
import com.raahmediq.bloodbank.web.BloodBankDtos.InventoryResponse;
import com.raahmediq.common.error.ConflictException;
import com.raahmediq.common.error.NotFoundException;
import com.raahmediq.doctor.domain.Doctor;
import com.raahmediq.doctor.repository.DoctorRepository;
import com.raahmediq.hospital.domain.Hospital;
import com.raahmediq.hospital.repository.HospitalRepository;
import com.raahmediq.notification.domain.NotificationType;
import com.raahmediq.notification.service.NotificationService;
import com.raahmediq.patient.domain.Patient;
import com.raahmediq.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BloodBankService {
    private static final Set<Role> REQUEST_CREATOR_ROLES = Set.of(
            Role.DOCTOR, Role.BLOOD_BANK_STAFF, Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);
    private static final Set<Role> BLOOD_STAFF_ROLES = Set.of(
            Role.BLOOD_BANK_STAFF, Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);

    private final BloodBankRepository bloodBanks;
    private final BloodInventoryBatchRepository inventory;
    private final BloodRequestRepository requests;
    private final BloodAllocationRepository allocations;
    private final DonorOptInRepository donors;
    private final HospitalRepository hospitals;
    private final PatientRepository patients;
    private final UserAccountRepository users;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final NotificationService notifications;
    private final AuditService audit;
    private final BloodBankProperties properties;
    private final Clock clock;

    public BloodBankService(BloodBankRepository bloodBanks, BloodInventoryBatchRepository inventory,
                            BloodRequestRepository requests, BloodAllocationRepository allocations,
                            DonorOptInRepository donors, HospitalRepository hospitals, PatientRepository patients,
                            UserAccountRepository users, DoctorRepository doctors,
                            AppointmentRepository appointments, NotificationService notifications,
                            AuditService audit, BloodBankProperties properties, Clock clock) {
        this.bloodBanks = bloodBanks;
        this.inventory = inventory;
        this.requests = requests;
        this.allocations = allocations;
        this.donors = donors;
        this.hospitals = hospitals;
        this.patients = patients;
        this.users = users;
        this.doctors = doctors;
        this.appointments = appointments;
        this.notifications = notifications;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BloodBankResponse> banks(UUID hospitalId) {
        requireHospital(hospitalId);
        return bloodBanks.findAllByHospitalIdAndAuthorizedTrueAndActiveTrueOrderByDistanceKmAscNameAsc(hospitalId)
                .stream().map(BloodBankService::toResponse).toList();
    }

    @Transactional
    public BloodBankResponse createBank(BloodBankRequest request) {
        Hospital hospital = requireHospital(request.hospitalId());
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (bloodBanks.existsByHospitalIdAndCodeIgnoreCase(hospital.getId(), code)) {
            throw new ConflictException("A blood bank already uses this code for the hospital.");
        }
        BloodBank bank = bloodBanks.save(new BloodBank(hospital, code, request.name().trim(),
                request.addressLine().trim(), request.contactNumber().trim(), request.distanceKm(),
                request.estimatedTransferMinutes(), request.sourceType()));
        audit.record("BLOOD_BANK_CREATED", "BLOOD_BANK", bank.getId(), hospital.getId());
        return toResponse(bank);
    }

    @Transactional
    public InventoryResponse verifyInventory(UUID staffUserId, InventoryRequest request) {
        UserAccount verifier = requireBloodStaff(staffUserId);
        BloodBank bank = requireBank(request.bloodBankId());
        Instant now = clock.instant();
        String batchReference = request.batchReference().trim().toUpperCase(Locale.ROOT);
        BloodInventoryBatch batch = inventory.findByBankAndBatchForUpdate(bank.getId(), batchReference)
                .map(existing -> {
                    if (existing.getBloodGroup() != request.bloodGroup()
                            || existing.getComponent() != request.component()) {
                        throw new ConflictException("A batch reference cannot change blood group or component.");
                    }
                    transition(() -> existing.verify(request.expiresOn(), request.totalUnits(),
                            request.verificationStatus(), now, verifier));
                    return existing;
                }).orElseGet(() -> inventory.save(new BloodInventoryBatch(bank, request.bloodGroup(),
                        request.component(), batchReference, request.expiresOn(), request.totalUnits(),
                        request.verificationStatus(), now, verifier)));
        audit.record("BLOOD_INVENTORY_VERIFIED", "BLOOD_INVENTORY_BATCH", batch.getId(),
                bank.getHospital().getId());
        return toResponse(batch);
    }

    @Transactional
    public List<InventoryResponse> inventory(UUID staffUserId, UUID hospitalId,
                                             BloodGroup bloodGroup, BloodComponent component) {
        requireBloodStaff(staffUserId);
        requireHospital(hospitalId);
        audit.record("BLOOD_INVENTORY_VIEWED", "HOSPITAL", hospitalId, hospitalId);
        return inventory.findAllByBloodBankHospitalIdAndBloodGroupAndComponent(hospitalId, bloodGroup, component)
                .stream().map(BloodBankService::toResponse).toList();
    }

    @Transactional
    public List<AvailabilityResponse> availability(UUID hospitalId, BloodGroup bloodGroup,
                                                   BloodComponent component, int requestedUnits) {
        requireHospital(hospitalId);
        Instant now = clock.instant();
        Instant cutoff = now.minus(properties.verificationMaxAge());
        LocalDate today = LocalDate.now(clock);
        List<BloodInventoryBatch> rows = inventory.findAllByBloodBankHospitalIdAndBloodGroupAndComponent(
                hospitalId, bloodGroup, component);
        List<AvailabilityResponse> response = bloodBanks
                .findAllByHospitalIdAndAuthorizedTrueAndActiveTrueOrderByDistanceKmAscNameAsc(hospitalId)
                .stream().map(bank -> {
                    List<BloodInventoryBatch> bankRows = rows.stream()
                            .filter(row -> row.getBloodBank().getId().equals(bank.getId())).toList();
                    Instant latest = bankRows.stream().map(BloodInventoryBatch::getLastVerifiedAt)
                            .max(Comparator.naturalOrder()).orElse(null);
                    int available = bankRows.stream().filter(row -> isFresh(row, cutoff, today))
                            .mapToInt(BloodInventoryBatch::availableUnits).sum();
                    boolean hasFreshVerification = bankRows.stream().anyMatch(row -> isFresh(row, cutoff, today));
                    AvailabilityStatus status = !hasFreshVerification ? AvailabilityStatus.STALE_OR_UNVERIFIED
                            : available >= requestedUnits ? AvailabilityStatus.AVAILABLE
                            : available > 0 ? AvailabilityStatus.LIMITED : AvailabilityStatus.UNAVAILABLE;
                    return new AvailabilityResponse(bank.getId(), bank.getName(), bank.getAddressLine(),
                            bank.getContactNumber(), bank.getSourceType(), bank.getDistanceKm(),
                            bank.getEstimatedTransferMinutes(), bloodGroup, component, status, available, latest,
                            latest == null ? null : latest.plus(properties.verificationMaxAge()));
                }).sorted(Comparator.comparingInt((AvailabilityResponse item) -> availabilityRank(item.status()))
                        .thenComparing(AvailabilityResponse::distanceKm)).toList();
        audit.record("BLOOD_AVAILABILITY_VIEWED", "HOSPITAL", hospitalId, hospitalId);
        return response;
    }

    @Transactional
    public BloodRequestResponse createRequest(UUID actorUserId, CreateBloodRequest request) {
        UserAccount actor = requireRequestCreator(actorUserId);
        BloodRequest existing = requests.findByCreatedByIdAndIdempotencyKey(actorUserId,
                request.idempotencyKey().trim()).orElse(null);
        if (existing != null) return toResponse(existing);
        Patient patient = patients.findByPatientNumberIgnoreCase(request.patientNumber().trim())
                .orElseThrow(() -> new NotFoundException("Patient was not found."));
        Hospital hospital = requireHospital(request.hospitalId());
        Appointment appointment = validateClinicalContext(actor, patient, hospital, request.appointmentId());
        BloodRequest bloodRequest = requests.save(new BloodRequest(patient, hospital, appointment, actor,
                request.bloodGroup(), request.component(), request.units(), request.urgency(),
                request.clinicalReason().trim(), request.idempotencyKey().trim()));
        audit.record("BLOOD_REQUEST_CREATED", "BLOOD_REQUEST", bloodRequest.getId(), hospital.getId());
        allocate(bloodRequest);
        notifications.notifyPatient(patient, hospital.getId(), NotificationType.BLOOD_REQUEST_CREATED,
                bloodRequest.getId(), "CREATED", "Blood support request created",
                "Your care team created a blood-support request. Open RaahMediQ Health for verified status updates.");
        notifyStatus(bloodRequest);
        return toResponse(bloodRequest);
    }

    @Transactional
    public BloodRequestResponse searchAgain(UUID staffUserId, UUID requestId) {
        requireBloodStaff(staffUserId);
        BloodRequest request = requireRequestForUpdate(requestId);
        if (request.getStatus() != BloodRequestStatus.UNAVAILABLE
                && request.getStatus() != BloodRequestStatus.PARTIALLY_RESERVED
                && request.getStatus() != BloodRequestStatus.SEARCHING) {
            throw new ConflictException("This blood request cannot be searched in its current state.");
        }
        int before = request.getMatchedUnits();
        allocate(request);
        audit.record("BLOOD_REQUEST_SEARCHED", "BLOOD_REQUEST", request.getId(), request.getHospital().getId());
        if (request.getMatchedUnits() != before) notifyStatus(request);
        return toResponse(request);
    }

    @Transactional
    public BloodRequestResponse fulfil(UUID staffUserId, UUID requestId) {
        requireBloodStaff(staffUserId);
        BloodRequest request = requireRequestForUpdate(requestId);
        Instant now = clock.instant();
        List<BloodAllocation> active = allocations.findAllByRequestIdAndStatus(
                requestId, BloodAllocationStatus.RESERVED);
        if (active.isEmpty()) throw new ConflictException("No active blood allocation exists.");
        transition(() -> request.fulfil(now));
        for (BloodAllocation allocation : active) {
            BloodInventoryBatch batch = inventory.findByIdForUpdate(allocation.getInventoryBatch().getId())
                    .orElseThrow(() -> new NotFoundException("Allocated inventory batch was not found."));
            transition(() -> batch.fulfil(allocation.getUnits()));
            transition(() -> allocation.fulfil(now));
        }
        audit.record("BLOOD_REQUEST_FULFILLED", "BLOOD_REQUEST", request.getId(), request.getHospital().getId());
        notifyStatus(request);
        return toResponse(request);
    }

    @Transactional
    public BloodRequestResponse cancel(UUID actorUserId, UUID requestId, CancelBloodRequest body) {
        UserAccount actor = requireRequestCreator(actorUserId);
        BloodRequest request = requireRequestForUpdate(requestId);
        boolean staff = hasAnyRole(actor, BLOOD_STAFF_ROLES);
        if (!staff && !request.getCreatedBy().getId().equals(actorUserId)) {
            throw new AccessDeniedException("Only the creating clinician or blood-bank staff can cancel this request.");
        }
        Instant now = clock.instant();
        for (BloodAllocation allocation : allocations.findAllByRequestIdAndStatus(
                requestId, BloodAllocationStatus.RESERVED)) {
            BloodInventoryBatch batch = inventory.findByIdForUpdate(allocation.getInventoryBatch().getId())
                    .orElseThrow(() -> new NotFoundException("Allocated inventory batch was not found."));
            transition(() -> batch.release(allocation.getUnits()));
            transition(() -> allocation.release(now));
        }
        transition(() -> request.cancel(now, body.reason().trim()));
        audit.record("BLOOD_REQUEST_CANCELLED", "BLOOD_REQUEST", request.getId(), request.getHospital().getId());
        notifyStatus(request);
        return toResponse(request);
    }

    @Transactional
    public List<BloodRequestResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        audit.record("BLOOD_REQUESTS_VIEWED", "PATIENT", patient.getId(), null);
        return requests.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public List<BloodRequestResponse> worklist(UUID staffUserId, UUID hospitalId, BloodRequestStatus status) {
        requireBloodStaff(staffUserId);
        requireHospital(hospitalId);
        audit.record("BLOOD_REQUEST_WORKLIST_VIEWED", "HOSPITAL", hospitalId, hospitalId);
        List<BloodRequest> matches = status == null
                ? requests.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId)
                : requests.findAllByHospitalIdAndStatusOrderByCreatedAtDesc(hospitalId, status);
        return matches.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DonorResponse consent(UUID userId, DonorConsentRequest request) {
        UserAccount user = requireUser(userId);
        Instant now = clock.instant();
        DonorOptIn donor = donors.findByUserId(userId).map(existing -> {
            existing.renewConsent(request.contactPreference(), now);
            return existing;
        }).orElseGet(() -> donors.save(new DonorOptIn(user, request.contactPreference(), now)));
        audit.record("BLOOD_DONOR_CONSENTED", "BLOOD_DONOR_OPT_IN", donor.getId(), null);
        return toResponse(donor);
    }

    @Transactional(readOnly = true)
    public DonorResponse donorMine(UUID userId) {
        return donors.findByUserId(userId).map(BloodBankService::toResponse).orElse(null);
    }

    @Transactional
    public DonorResponse withdrawConsent(UUID userId) {
        DonorOptIn donor = donors.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No donor consent was found."));
        donor.withdraw(clock.instant());
        audit.record("BLOOD_DONOR_CONSENT_WITHDRAWN", "BLOOD_DONOR_OPT_IN", donor.getId(), null);
        return toResponse(donor);
    }

    @Transactional
    public DonorResponse verifyDonor(UUID staffUserId, UUID donorId, DonorVerificationRequest request) {
        UserAccount staff = requireBloodStaff(staffUserId);
        DonorOptIn donor = donors.findById(donorId)
                .orElseThrow(() -> new NotFoundException("Donor consent was not found."));
        transition(() -> donor.verify(request.verifiedBloodGroup(), request.eligibilityStatus(),
                clock.instant(), staff));
        audit.record("BLOOD_DONOR_ELIGIBILITY_VERIFIED", "BLOOD_DONOR_OPT_IN", donor.getId(), null);
        return toResponse(donor);
    }

    @Transactional
    public List<DonorMatchResponse> donorMatches(UUID staffUserId, UUID requestId) {
        requireBloodStaff(staffUserId);
        BloodRequest request = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Blood request was not found."));
        if (request.getStatus() != BloodRequestStatus.UNAVAILABLE
                && request.getStatus() != BloodRequestStatus.PARTIALLY_RESERVED) {
            throw new ConflictException("Donor matching is available only after verified inventory is insufficient.");
        }
        List<DonorMatchResponse> matches = donors.findAllByVerifiedBloodGroupAndEligibilityStatus(
                        request.getBloodGroup(), DonorEligibilityStatus.ELIGIBLE).stream()
                .filter(donor -> !donor.getUser().getId().equals(request.getPatient().getUser().getId()))
                .map(donor -> new DonorMatchResponse(donor.getId(), donor.getUser().getDisplayName(),
                        donor.getUser().getMobileNumber(), donor.getContactPreference(), donor.getVerifiedBloodGroup(),
                        donor.getEligibilityVerifiedAt())).toList();
        audit.record("BLOOD_DONOR_MATCHES_VIEWED", "BLOOD_REQUEST", request.getId(), request.getHospital().getId());
        return matches;
    }

    private void allocate(BloodRequest request) {
        int remaining = request.getRequestedUnits() - request.getMatchedUnits();
        if (remaining <= 0) return;
        Instant now = clock.instant();
        List<BloodInventoryBatch> candidates = inventory.findEligibleForUpdate(request.getHospital().getId(),
                request.getBloodGroup(), request.getComponent(), InventoryVerificationStatus.VERIFIED,
                now.minus(properties.verificationMaxAge()), LocalDate.now(clock));
        Map<UUID, BloodAllocation> existing = new HashMap<>();
        allocations.findAllByRequestIdAndStatus(request.getId(), BloodAllocationStatus.RESERVED)
                .forEach(item -> existing.put(item.getInventoryBatch().getId(), item));
        for (BloodInventoryBatch batch : candidates) {
            if (remaining == 0) break;
            int reserved = batch.reserve(remaining);
            if (reserved == 0) continue;
            BloodAllocation current = existing.get(batch.getId());
            if (current == null) {
                current = allocations.save(new BloodAllocation(request, batch, reserved, now));
                existing.put(batch.getId(), current);
            } else {
                current.addUnits(reserved);
            }
            request.addMatchedUnits(reserved);
            remaining -= reserved;
        }
        if (remaining > 0) request.markUnavailable();
    }

    private Appointment validateClinicalContext(UserAccount actor, Patient patient, Hospital hospital,
                                                UUID appointmentId) {
        Appointment appointment = appointmentId == null ? null : appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
        if (appointment != null && (!appointment.getPatient().getId().equals(patient.getId())
                || !appointment.getHospital().getId().equals(hospital.getId()))) {
            throw new AccessDeniedException("Appointment, patient, and hospital do not match.");
        }
        if (actor.getRoles().contains(Role.DOCTOR) && !hasAnyRole(actor, BLOOD_STAFF_ROLES)) {
            Doctor doctor = doctors.findByLinkedUserId(actor.getId()).filter(Doctor::isActive)
                    .orElseThrow(() -> new AccessDeniedException("A linked active doctor profile is required."));
            if (!doctor.getHospital().getId().equals(hospital.getId())) {
                throw new AccessDeniedException("The clinician is linked to another hospital.");
            }
            if (appointment != null && !appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("The appointment is assigned to another doctor.");
            }
        }
        return appointment;
    }

    private BloodRequestResponse toResponse(BloodRequest request) {
        List<AllocationResponse> allocationResponses = allocations.findAllByRequestIdOrderByCreatedAtAsc(
                request.getId()).stream().map(item -> {
            BloodBank bank = item.getInventoryBatch().getBloodBank();
            return new AllocationResponse(item.getId(), bank.getId(), bank.getName(), bank.getContactNumber(),
                    bank.getDistanceKm(), bank.getEstimatedTransferMinutes(), item.getUnits(),
                    item.getInventoryBatch().getExpiresOn(), item.getStatus().name());
        }).toList();
        return new BloodRequestResponse(request.getId(), request.getPatient().getPatientNumber(),
                request.getHospital().getId(), request.getHospital().getName(),
                request.getAppointment() == null ? null : request.getAppointment().getId(),
                request.getCreatedBy().getDisplayName(), request.getBloodGroup(), request.getComponent(),
                request.getRequestedUnits(), request.getMatchedUnits(), request.getUrgency(), request.getStatus(),
                request.getClinicalReason(), request.getCreatedAt(), request.getResolvedAt(),
                request.getCancellationReason(), allocationResponses);
    }

    private void notifyStatus(BloodRequest request) {
        String message = switch (request.getStatus()) {
            case RESERVED -> "Verified blood-bank inventory has been reserved. Your care team will coordinate fulfilment.";
            case PARTIALLY_RESERVED -> "Part of the requested inventory is reserved. Authorized staff are continuing the search.";
            case UNAVAILABLE -> "No recently verified matching inventory is currently available in the authorized network. Staff review is required.";
            case FULFILLED -> "The blood-bank request has been fulfilled by authorized staff.";
            case CANCELLED -> "The blood-bank request was cancelled by an authorized care-team member.";
            case SEARCHING -> "Authorized staff are searching recently verified inventory.";
        };
        notifications.notifyPatient(request.getPatient(), request.getHospital().getId(),
                NotificationType.BLOOD_REQUEST_UPDATED, request.getId(), request.getStatus().name(),
                "Blood support status updated", message);
    }

    private static BloodBankResponse toResponse(BloodBank bank) {
        return new BloodBankResponse(bank.getId(), bank.getHospital().getId(), bank.getHospital().getName(),
                bank.getCode(), bank.getName(), bank.getAddressLine(), bank.getContactNumber(),
                bank.getDistanceKm(), bank.getEstimatedTransferMinutes(), bank.getSourceType());
    }

    private static InventoryResponse toResponse(BloodInventoryBatch batch) {
        return new InventoryResponse(batch.getId(), batch.getBloodBank().getId(), batch.getBloodBank().getName(),
                batch.getBloodGroup(), batch.getComponent(), batch.getBatchReference(), batch.getExpiresOn(),
                batch.getTotalUnits(), batch.getReservedUnits(), batch.availableUnits(),
                batch.getVerificationStatus(), batch.getLastVerifiedAt(), batch.getVerifiedBy().getDisplayName());
    }

    private static DonorResponse toResponse(DonorOptIn donor) {
        return new DonorResponse(donor.getId(), donor.getEligibilityStatus(), donor.getVerifiedBloodGroup(),
                donor.getContactPreference(), donor.getConsentedAt(), donor.getEligibilityVerifiedAt(),
                donor.getEligibilityStatus() != DonorEligibilityStatus.WITHDRAWN);
    }

    private Hospital requireHospital(UUID id) {
        return hospitals.findById(id).filter(Hospital::isActive)
                .orElseThrow(() -> new NotFoundException("Hospital was not found."));
    }

    private BloodBank requireBank(UUID id) {
        return bloodBanks.findById(id).filter(BloodBank::isActive).filter(BloodBank::isAuthorized)
                .orElseThrow(() -> new NotFoundException("Authorized blood bank was not found."));
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }

    private UserAccount requireUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User account is unavailable."));
    }

    private UserAccount requireRequestCreator(UUID userId) {
        UserAccount user = requireUser(userId);
        if (!hasAnyRole(user, REQUEST_CREATOR_ROLES)) {
            throw new AccessDeniedException("A clinician or authorized blood-bank role is required.");
        }
        return user;
    }

    private UserAccount requireBloodStaff(UUID userId) {
        UserAccount user = requireUser(userId);
        if (!hasAnyRole(user, BLOOD_STAFF_ROLES)) {
            throw new AccessDeniedException("An authorized blood-bank staff role is required.");
        }
        return user;
    }

    private BloodRequest requireRequestForUpdate(UUID id) {
        return requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Blood request was not found."));
    }

    private static boolean hasAnyRole(UserAccount user, Set<Role> roles) {
        return user.getRoles().stream().anyMatch(roles::contains);
    }

    private static boolean isFresh(BloodInventoryBatch item, Instant cutoff, LocalDate today) {
        return item.getVerificationStatus() == InventoryVerificationStatus.VERIFIED
                && !item.getLastVerifiedAt().isBefore(cutoff)
                && !item.getExpiresOn().isBefore(today);
    }

    private static int availabilityRank(AvailabilityStatus status) {
        return switch (status) {
            case AVAILABLE -> 0;
            case LIMITED -> 1;
            case UNAVAILABLE -> 2;
            case STALE_OR_UNVERIFIED -> 3;
        };
    }

    private static void transition(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }
}
