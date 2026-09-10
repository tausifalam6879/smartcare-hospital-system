package com.smartcare.operations.service;

import com.smartcare.ambulance.domain.AmbulanceStatus;
import com.smartcare.ambulance.repository.AmbulanceRepository;
import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.repository.AppointmentRepository;
import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.bloodbank.domain.InventoryVerificationStatus;
import com.smartcare.bloodbank.repository.BloodInventoryBatchRepository;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.diagnostic.repository.DiagnosticOrderRepository;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.notification.domain.DeliveryStatus;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.repository.NotificationDeliveryRepository;
import com.smartcare.notification.service.NotificationService;
import com.smartcare.operations.domain.AppointmentRecoveryCase;
import com.smartcare.operations.domain.DoctorDayOperation;
import com.smartcare.operations.domain.DoctorDayStatus;
import com.smartcare.operations.domain.RecoveryChoice;
import com.smartcare.operations.domain.RecoveryStatus;
import com.smartcare.operations.repository.AppointmentRecoveryCaseRepository;
import com.smartcare.operations.repository.DoctorDayOperationRepository;
import com.smartcare.operations.web.OperationsDtos.DoctorDayStatusResponse;
import com.smartcare.operations.web.OperationsDtos.OperationsDashboardResponse;
import com.smartcare.operations.web.OperationsDtos.QueueRow;
import com.smartcare.operations.web.OperationsDtos.RecoveryCaseResponse;
import com.smartcare.operations.web.OperationsDtos.ResolveRecovery;
import com.smartcare.operations.web.OperationsDtos.UpdateDoctorDayStatus;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import com.smartcare.payment.domain.PaymentStatus;
import com.smartcare.payment.repository.PaymentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HospitalOperationsService {
    private static final Set<Role> VIEW_ROLES = Set.of(Role.DOCTOR, Role.RECEPTIONIST, Role.CASHIER,
            Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);
    private static final Set<Role> CONTROL_ROLES = Set.of(Role.DOCTOR, Role.RECEPTIONIST,
            Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);
    private static final EnumSet<AppointmentStatus> AFFECTED_STATUSES = EnumSet.of(
            AppointmentStatus.RESERVED_PENDING_PAYMENT, AppointmentStatus.CASH_PENDING,
            AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);
    private static final EnumSet<AppointmentStatus> RECOVERY_STATUSES = EnumSet.of(
            AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);

    private final DoctorDayOperationRepository operations;
    private final AppointmentRecoveryCaseRepository recoveryCases;
    private final AppointmentRepository appointments;
    private final AppointmentService appointmentService;
    private final DoctorRepository doctors;
    private final HospitalRepository hospitals;
    private final PatientRepository patients;
    private final UserAccountRepository users;
    private final DiagnosticOrderRepository diagnostics;
    private final BloodInventoryBatchRepository bloodInventory;
    private final AmbulanceRepository ambulances;
    private final PaymentRepository payments;
    private final NotificationDeliveryRepository deliveries;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public HospitalOperationsService(DoctorDayOperationRepository operations,
                                     AppointmentRecoveryCaseRepository recoveryCases,
                                     AppointmentRepository appointments, AppointmentService appointmentService,
                                     DoctorRepository doctors, HospitalRepository hospitals,
                                     PatientRepository patients, UserAccountRepository users,
                                     DiagnosticOrderRepository diagnostics,
                                     BloodInventoryBatchRepository bloodInventory,
                                     AmbulanceRepository ambulances, PaymentRepository payments,
                                     NotificationDeliveryRepository deliveries,
                                     NotificationService notifications, AuditService audit, Clock clock) {
        this.operations = operations;
        this.recoveryCases = recoveryCases;
        this.appointments = appointments;
        this.appointmentService = appointmentService;
        this.doctors = doctors;
        this.hospitals = hospitals;
        this.patients = patients;
        this.users = users;
        this.diagnostics = diagnostics;
        this.bloodInventory = bloodInventory;
        this.ambulances = ambulances;
        this.payments = payments;
        this.deliveries = deliveries;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public DoctorDayStatusResponse updateDoctorStatus(UUID userId, UpdateDoctorDayStatus input) {
        UserAccount actor = requireRole(userId, CONTROL_ROLES);
        Doctor doctor = doctors.findByIdForUpdate(input.doctorId())
                .orElseThrow(() -> new NotFoundException("Doctor was not found."));
        authorizeDoctorScope(actor, doctor);
        requireReason(input.status(), input.reason());
        Instant now = clock.instant();
        DoctorDayOperation operation = operations.findForUpdate(doctor.getId(), input.serviceDate())
                .map(existing -> {
                    existing.update(input.status(), blankToNull(input.reason()), actor, now);
                    return existing;
                }).orElseGet(() -> operations.save(new DoctorDayOperation(doctor, input.serviceDate(),
                        input.status(), blankToNull(input.reason()), actor, now)));

        List<Appointment> affected = appointments.findAllByDoctorIdAndServiceDateAndStatusInOrderByQueuePositionAsc(
                doctor.getId(), input.serviceDate(), AFFECTED_STATUSES);
        for (Appointment appointment : affected) {
            NotificationType type = input.status() == DoctorDayStatus.CANCELLED_FOR_DAY
                    ? NotificationType.DOCTOR_UNAVAILABLE : NotificationType.DOCTOR_DELAYED;
            notifications.notifyAppointment(appointment, type,
                    input.status() + ":" + now.toEpochMilli(), statusTitle(input.status()),
                    statusMessage(input.status(), operation.getDelayMinutes(), operation.getReason()));
            if (input.status() == DoctorDayStatus.CANCELLED_FOR_DAY
                    && RECOVERY_STATUSES.contains(appointment.getStatus())
                    && recoveryCases.findByAppointmentId(appointment.getId()).isEmpty()) {
                AppointmentRecoveryCase recovery = recoveryCases.save(new AppointmentRecoveryCase(appointment, operation));
                notifications.notifyAppointment(appointment, NotificationType.APPOINTMENT_RECOVERY_REQUIRED,
                        "recovery:" + recovery.getId(), "Your approval is required",
                        "The doctor is unavailable. Choose rescheduling or refund review; SmartCare will not move you automatically.");
            }
        }
        audit.record("DOCTOR_DAY_STATUS_UPDATED", "DOCTOR_DAY_OPERATION", operation.getId(),
                doctor.getHospital().getId());
        return toResponse(operation);
    }

    @Transactional
    public void markNoShow(UUID userId, UUID appointmentId) {
        UserAccount actor = requireRole(userId, CONTROL_ROLES);
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
        authorizeDoctorScope(actor, appointment.getDoctor());
        appointmentService.markNoShowOperational(appointmentId);
    }

    @Transactional(readOnly = true)
    public List<RecoveryCaseResponse> recoveryMine(UUID userId) {
        Patient patient = requirePatient(userId);
        return recoveryCases.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(HospitalOperationsService::toResponse).toList();
    }

    @Transactional
    public RecoveryCaseResponse resolveRecovery(UUID userId, UUID caseId, ResolveRecovery input) {
        Patient patient = requirePatient(userId);
        AppointmentRecoveryCase recovery = recoveryCases.findByIdForUpdate(caseId)
                .orElseThrow(() -> new NotFoundException("Appointment recovery case was not found."));
        if (!recovery.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("This recovery case belongs to another patient.");
        }
        if (recovery.getStatus() != RecoveryStatus.AWAITING_PATIENT_CHOICE) {
            throw new ConflictException("This recovery case has already been resolved.");
        }
        Instant now = clock.instant();
        if (input.choice() == RecoveryChoice.REFUND_REVIEW) {
            appointmentService.cancelForDoctorUnavailability(recovery.getAppointment().getId());
            transition(() -> recovery.refundReview(now));
            audit.record("APPOINTMENT_RECOVERY_REFUND_REVIEW", "APPOINTMENT_RECOVERY_CASE", recovery.getId(),
                    recovery.getAppointment().getHospital().getId());
            return toResponse(recovery);
        }
        if (input.targetDate() == null) throw new IllegalArgumentException("A target date is required.");
        Doctor original = recovery.getAppointment().getDoctor();
        Doctor target;
        if (input.choice() == RecoveryChoice.MOVE_TO_ELIGIBLE_DOCTOR) {
            if (input.targetDoctorId() == null) throw new IllegalArgumentException("A target doctor is required.");
            target = doctors.findById(input.targetDoctorId())
                    .orElseThrow(() -> new NotFoundException("Target doctor was not found."));
            if (!target.getHospital().getId().equals(original.getHospital().getId())
                    || !target.getDepartment().getId().equals(original.getDepartment().getId())
                    || !target.isActive()) {
                throw new ConflictException("The target must be an active doctor in the same hospital and department.");
            }
        } else {
            target = original;
        }
        appointmentService.rescheduleAfterPatientApproval(recovery.getAppointment().getId(),
                target.getId(), input.targetDate());
        transition(() -> recovery.rescheduled(input.choice(), target, input.targetDate(), now));
        audit.record("APPOINTMENT_RECOVERY_APPROVED", "APPOINTMENT_RECOVERY_CASE", recovery.getId(),
                recovery.getAppointment().getHospital().getId());
        return toResponse(recovery);
    }

    @Transactional(readOnly = true)
    public OperationsDashboardResponse dashboard(UUID userId, UUID hospitalId, LocalDate date) {
        UserAccount actor = requireRole(userId, VIEW_ROLES);
        Hospital hospital = hospitals.findById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital was not found."));
        Doctor scopedDoctor = actor.getRoles().contains(Role.DOCTOR)
                && actor.getRoles().stream().noneMatch(role -> role == Role.HOSPITAL_ADMIN || role == Role.SUPER_ADMIN
                || role == Role.RECEPTIONIST || role == Role.CASHIER)
                ? doctors.findByLinkedUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A linked doctor account is required.")) : null;
        if (scopedDoctor != null && !scopedDoctor.getHospital().getId().equals(hospitalId)) {
            throw new AccessDeniedException("Doctor is linked to another hospital.");
        }
        List<Appointment> rows = appointments.findAllByHospitalIdAndServiceDateOrderByDoctorNameAscQueuePositionAsc(
                hospitalId, date).stream().filter(item -> scopedDoctor == null
                        || item.getDoctor().getId().equals(scopedDoctor.getId())).toList();
        List<Doctor> activeDoctors = scopedDoctor == null
                ? doctors.findAllByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId) : List.of(scopedDoctor);
        List<DoctorDayOperation> dayOperations = operations
                .findAllByHospitalIdAndServiceDateOrderByDoctorNameAsc(hospitalId, date).stream()
                .filter(item -> scopedDoctor == null || item.getDoctor().getId().equals(scopedDoctor.getId())).toList();
        int pendingPayments = (int) payments.findAll().stream().filter(payment ->
                payment.getAppointment().getHospital().getId().equals(hospitalId)
                        && payment.getAppointment().getServiceDate().equals(date)
                        && (payment.getStatus() == PaymentStatus.PENDING
                        || payment.getStatus() == PaymentStatus.REFUND_PENDING)).count();
        int bloodAlerts = (int) bloodInventory.findAllByBloodBankHospitalId(hospitalId).stream().filter(batch ->
                batch.getVerificationStatus() != InventoryVerificationStatus.VERIFIED
                        || !batch.getExpiresOn().isAfter(date)
                        || batch.getLastVerifiedAt().isBefore(clock.instant().minus(Duration.ofHours(6)))
                        || batch.availableUnits() <= 2).count();
        int recordedWait = (int) rows.stream().filter(item -> item.getCheckedInAt() != null
                && item.getConsultationStartedAt() != null).mapToLong(item -> Duration.between(
                        item.getCheckedInAt(), item.getConsultationStartedAt()).toMinutes()).average().orElse(0);
        Long failures = actor.getRoles().contains(Role.SUPER_ADMIN)
                ? deliveries.countByStatus(DeliveryStatus.FAILED) : null;
        List<QueueRow> queue = rows.stream().map(item -> new QueueRow(item.getId(),
                item.getPatient().getPatientNumber(), item.getDoctor().getId(), item.getDoctor().getName(),
                item.getQueuePosition(), item.getStatus(), item.getPaymentMethod())).toList();
        return new OperationsDashboardResponse(hospitalId, hospital.getName(), date, rows.size(),
                count(rows, AppointmentStatus.CONFIRMED), count(rows, AppointmentStatus.CHECKED_IN),
                count(rows, AppointmentStatus.IN_CONSULTATION), count(rows, AppointmentStatus.COMPLETED),
                count(rows, AppointmentStatus.WAITLISTED), count(rows, AppointmentStatus.NO_SHOW),
                pendingPayments, activeDoctors.size(), (int) dayOperations.stream()
                .filter(item -> item.getStatus() != DoctorDayStatus.ON_TIME).count(),
                diagnostics.findAllByProcedureHospitalIdAndScheduledDateOrderByQueuePositionAsc(hospitalId, date).size(),
                bloodAlerts,
                ambulances.countByHospitalIdAndActiveTrueAndStatus(hospitalId, AmbulanceStatus.AVAILABLE),
                ambulances.countByHospitalIdAndActiveTrueAndStatus(hospitalId, AmbulanceStatus.OUT_OF_SERVICE),
                failures, recordedWait, dayOperations.stream().map(HospitalOperationsService::toResponse).toList(),
                queue);
    }

    private void authorizeDoctorScope(UserAccount actor, Doctor doctor) {
        if (actor.getRoles().contains(Role.DOCTOR)
                && actor.getRoles().stream().noneMatch(role -> role == Role.HOSPITAL_ADMIN
                || role == Role.SUPER_ADMIN || role == Role.RECEPTIONIST)) {
            Doctor linked = doctors.findByLinkedUserId(actor.getId())
                    .orElseThrow(() -> new AccessDeniedException("A linked doctor account is required."));
            if (!linked.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can update only their own operating status.");
            }
        }
    }

    private static int count(List<Appointment> rows, AppointmentStatus status) {
        return (int) rows.stream().filter(item -> item.getStatus() == status).count();
    }

    private UserAccount requireRole(UUID id, Set<Role> permitted) {
        UserAccount user = users.findById(id).orElseThrow(() -> new AccessDeniedException("Account was not found."));
        if (user.getRoles().stream().noneMatch(permitted::contains)) throw new AccessDeniedException("Staff role required.");
        return user;
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }

    private static void requireReason(DoctorDayStatus status, String reason) {
        if (status != DoctorDayStatus.ON_TIME && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A short operational reason is required for this status.");
        }
    }

    private static String statusTitle(DoctorDayStatus status) {
        return switch (status) {
            case ON_TIME -> "Doctor schedule restored";
            case DELAYED_30, DELAYED_60 -> "Doctor delay reported";
            case EMERGENCY_INTERRUPTION -> "Emergency interruption reported";
            case TEMPORARILY_UNAVAILABLE -> "Doctor temporarily unavailable";
            case CANCELLED_FOR_DAY -> "Doctor unavailable today";
        };
    }

    private static String statusMessage(DoctorDayStatus status, int delay, String reason) {
        String prefix = switch (status) {
            case ON_TIME -> "The hospital reports that the doctor is on schedule.";
            case DELAYED_30, DELAYED_60 -> "The current estimate includes an additional " + delay + " minute delay.";
            case EMERGENCY_INTERRUPTION -> "An emergency interruption may affect the queue estimate.";
            case TEMPORARILY_UNAVAILABLE -> "The doctor is temporarily unavailable; await a hospital update.";
            case CANCELLED_FOR_DAY -> "The doctor is unavailable today. Your appointment will not move without your approval.";
        };
        return reason == null ? prefix : prefix + " Operational note: " + reason;
    }

    private static DoctorDayStatusResponse toResponse(DoctorDayOperation item) {
        return new DoctorDayStatusResponse(item.getId(), item.getDoctor().getId(), item.getDoctor().getName(),
                item.getHospital().getId(), item.getServiceDate(), item.getStatus(), item.getDelayMinutes(),
                item.getReason(), item.getOperationalUpdatedAt());
    }

    private static RecoveryCaseResponse toResponse(AppointmentRecoveryCase item) {
        Appointment appointment = item.getAppointment();
        Doctor target = item.getTargetDoctor();
        return new RecoveryCaseResponse(item.getId(), appointment.getId(), item.getStatus(),
                item.getPatientChoice(), appointment.getHospital().getId(), appointment.getHospital().getName(),
                appointment.getDoctor().getDepartment().getId(), appointment.getDoctor().getDepartment().getName(),
                item.getOperation().getDoctor().getId(), item.getOperation().getDoctor().getName(),
                item.getOperation().getServiceDate(), item.getOriginalQueuePosition(), item.getOperation().getReason(),
                target == null ? null : target.getId(), target == null ? null : target.getName(),
                item.getTargetDate(), item.getDecidedAt(), item.getCreatedAt());
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
