package com.smartcare.appointment.service;

import com.smartcare.appointment.config.QueueProperties;
import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.domain.DoctorDayLedger;
import com.smartcare.appointment.domain.PaymentMethod;
import com.smartcare.appointment.domain.WaitlistEntry;
import com.smartcare.appointment.domain.WaitlistStatus;
import com.smartcare.appointment.repository.AppointmentRepository;
import com.smartcare.appointment.repository.DoctorDayLedgerRepository;
import com.smartcare.appointment.repository.WaitlistEntryRepository;
import com.smartcare.appointment.web.AppointmentDtos.AppointmentResponse;
import com.smartcare.appointment.web.AppointmentDtos.AvailabilityResponse;
import com.smartcare.appointment.web.AppointmentDtos.BookingRequest;
import com.smartcare.audit.service.AuditService;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.domain.DoctorSchedule;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.doctor.repository.DoctorScheduleRepository;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final EnumSet<AppointmentStatus> DUPLICATE_BLOCKING_STATUSES = EnumSet.of(
            AppointmentStatus.WAITLISTED,
            AppointmentStatus.RESERVED_PENDING_PAYMENT,
            AppointmentStatus.CASH_PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN,
            AppointmentStatus.IN_CONSULTATION);
    private static final EnumSet<AppointmentStatus> CANCELLABLE_STATUSES = EnumSet.of(
            AppointmentStatus.WAITLISTED, AppointmentStatus.RESERVED_PENDING_PAYMENT,
            AppointmentStatus.CASH_PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointments;
    private final DoctorDayLedgerRepository ledgers;
    private final WaitlistEntryRepository waitlist;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final QueueProperties properties;
    private final AuditService audit;
    private final NotificationService notifications;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, DoctorDayLedgerRepository ledgers,
                              WaitlistEntryRepository waitlist, PatientRepository patients,
                              DoctorRepository doctors, DoctorScheduleRepository schedules,
                              QueueProperties properties, AuditService audit,
                              NotificationService notifications, Clock clock) {
        this.appointments = appointments;
        this.ledgers = ledgers;
        this.waitlist = waitlist;
        this.patients = patients;
        this.doctors = doctors;
        this.schedules = schedules;
        this.properties = properties;
        this.audit = audit;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID doctorId, LocalDate serviceDate) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor was not found."));
        DailySchedule daily = requireBookableDay(doctor, serviceDate);
        DoctorDayLedger ledger = ledgers.findByDoctorIdAndServiceDate(doctorId, serviceDate).orElse(null);
        int reserved = ledger == null ? 0 : ledger.getActiveCount();
        int capacity = ledger == null ? daily.capacity() : ledger.getEffectiveCapacity();
        return new AvailabilityResponse(doctorId, serviceDate, true, capacity, reserved,
                Math.max(0, capacity - reserved),
                waitlist.countByDoctorIdAndServiceDateAndStatus(doctorId, serviceDate, WaitlistStatus.WAITING),
                daily.start(), daily.end());
    }

    @Transactional
    public AppointmentResponse book(UUID userId, String idempotencyKey, BookingRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("A valid Idempotency-Key header is required.");
        }
        Patient patient = requirePatient(userId);
        String normalizedKey = idempotencyKey.trim();
        var previous = appointments.findByPatientIdAndIdempotencyKey(patient.getId(), normalizedKey);
        if (previous.isPresent()) return toResponse(previous.get());

        Doctor doctor = doctors.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new NotFoundException("Doctor was not found."));
        DailySchedule daily = requireBookableDay(doctor, request.serviceDate());
        appointments.findFirstByPatientIdAndDoctorIdAndServiceDateAndStatusIn(
                        patient.getId(), doctor.getId(), request.serviceDate(), DUPLICATE_BLOCKING_STATUSES)
                .ifPresent(existing -> {
                    throw new ConflictException("You already have an active booking with this doctor on this date.");
                });

        DoctorDayLedger ledger = ledgers.findForUpdate(doctor.getId(), request.serviceDate())
                .orElseGet(() -> ledgers.save(new DoctorDayLedger(doctor, request.serviceDate(), daily.capacity())));
        Appointment appointment = appointments.save(new Appointment(patient, doctor, request.serviceDate(),
                request.paymentMethod(), doctor.getConsultationFee(), normalizedKey));
        Integer position = ledger.allocate();
        Instant now = clock.instant();
        if (position == null) {
            long sequence = waitlist.countByDoctorIdAndServiceDate(doctor.getId(), request.serviceDate()) + 1;
            waitlist.save(new WaitlistEntry(appointment, sequence));
            audit.record("APPOINTMENT_WAITLISTED", "APPOINTMENT", appointment.getId(), doctor.getHospital().getId());
            notifications.notifyAppointment(appointment, NotificationType.WAITLISTED, "booked",
                    "You are on the waitlist", "We will notify you if an OPD position becomes available.");
        } else {
            appointment.reserve(position, now.plus(properties.onlineReservationTtl()),
                    cashDeadline(doctor, request.serviceDate(), daily.start(), now));
            audit.record("QUEUE_POSITION_RESERVED", "APPOINTMENT", appointment.getId(), doctor.getHospital().getId());
            notifications.notifyAppointment(appointment, NotificationType.APPOINTMENT_RESERVED, "booked",
                    "OPD position reserved", "Your provisional OPD number is " + position + ".");
            if (appointment.getPaymentMethod() == PaymentMethod.ONLINE) {
                notifications.notifyAppointment(appointment, NotificationType.PAYMENT_REQUIRED, "online",
                        "Payment verification required", "Complete the verified online payment flow before the reservation expires.");
            } else {
                notifications.notifyAppointment(appointment, NotificationType.CASH_PAYMENT_DEADLINE, "cash",
                        "Cash confirmation required", "Pay at the authorized hospital desk before the displayed deadline.");
            }
        }
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> doctorMine(UUID userId) {
        Doctor doctor = doctors.findByLinkedUserId(userId).filter(Doctor::isActive)
                .orElseThrow(() -> new AccessDeniedException("A linked active doctor profile is required."));
        return appointments.findAllByDoctorIdOrderByServiceDateDescCreatedAtDesc(doctor.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        return appointments.findAllByPatientIdOrderByServiceDateDescCreatedAtDesc(patient.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse cancel(UUID userId, UUID appointmentId, String reason) {
        Patient patient = requirePatient(userId);
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Appointment belongs to another patient.");
        }
        if (!CANCELLABLE_STATUSES.contains(appointment.getStatus())) {
            throw new ConflictException("This appointment can no longer be cancelled.");
        }
        doctors.findByIdForUpdate(appointment.getDoctor().getId())
                .orElseThrow(() -> new NotFoundException("Doctor was not found."));
        appointment = requireAppointmentForUpdate(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            DailySchedule daily = dailySchedule(appointment.getDoctor(), appointment.getServiceDate());
            ZoneId zone = ZoneId.of(appointment.getHospital().getTimeZone());
            Instant closesAt = LocalDateTime.of(appointment.getServiceDate(), daily.start()).atZone(zone).toInstant()
                    .minus(properties.confirmedCancellationCutoff());
            if (!clock.instant().isBefore(closesAt)) {
                throw new ConflictException("The cancellation window for this confirmed appointment has closed.");
            }
        }
        if (appointment.getStatus() == AppointmentStatus.WAITLISTED) {
            waitlist.findByAppointmentId(appointment.getId()).ifPresent(WaitlistEntry::cancel);
        } else {
            releaseAndPromote(appointment);
        }
        appointment.cancel(clock.instant(), reason == null || reason.isBlank() ? "Cancelled by patient" : reason.trim());
        audit.record("APPOINTMENT_CANCELLED", "APPOINTMENT", appointment.getId(), appointment.getHospital().getId());
        notifications.notifyAppointment(appointment, NotificationType.APPOINTMENT_CANCELLED, "cancelled",
                "Appointment cancelled", "Your OPD booking was cancelled. Any eligible refund is tracked separately.");
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse confirmCash(UUID appointmentId) {
        Appointment appointment = requireAppointmentForUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CASH_PENDING) {
            throw new ConflictException("Only a cash-pending appointment can be confirmed here.");
        }
        Instant now = clock.instant();
        if (appointment.getCashDeadlineAt() != null && !appointment.getCashDeadlineAt().isAfter(now)) {
            throw new ConflictException("The cash confirmation deadline has passed.");
        }
        appointment.confirm(now);
        audit.record("CASH_APPOINTMENT_CONFIRMED", "APPOINTMENT", appointment.getId(), appointment.getHospital().getId());
        notifyConfirmed(appointment, "cash");
        return toResponse(appointment);
    }

    @Transactional
    public Appointment ownedOnlineReservation(UUID userId, UUID appointmentId) {
        Patient patient = requirePatient(userId);
        Appointment appointment = requireAppointmentForUpdate(appointmentId);
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Appointment belongs to another patient.");
        }
        if (appointment.getPaymentMethod() != PaymentMethod.ONLINE
                || appointment.getStatus() != AppointmentStatus.RESERVED_PENDING_PAYMENT) {
            throw new ConflictException("This appointment is not awaiting an online payment.");
        }
        if (appointment.getReservationExpiresAt() == null
                || !appointment.getReservationExpiresAt().isAfter(clock.instant())) {
            throw new ConflictException("The online reservation has expired.");
        }
        return appointment;
    }

    @Transactional
    public Appointment appointmentForPayment(UUID appointmentId) {
        return requireAppointmentForUpdate(appointmentId);
    }

    @Transactional
    public boolean confirmOnlinePayment(UUID appointmentId, Instant verifiedAt) {
        Appointment appointment = requireAppointmentForUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.RESERVED_PENDING_PAYMENT
                || appointment.getReservationExpiresAt() == null
                || !appointment.getReservationExpiresAt().isAfter(verifiedAt)) {
            return false;
        }
        appointment.confirm(verifiedAt);
        audit.recordAs("PAYMENT_WEBHOOK", "ONLINE_APPOINTMENT_CONFIRMED", "APPOINTMENT", appointment.getId(),
                appointment.getHospital().getId());
        notifyConfirmed(appointment, "online");
        return true;
    }

    @Transactional
    public void rescheduleAfterPatientApproval(UUID appointmentId, UUID targetDoctorId, LocalDate targetDate) {
        Appointment appointment = requireAppointmentForUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new ConflictException("Only a confirmed or checked-in appointment can be recovered.");
        }
        if (appointment.getDoctor().getId().equals(targetDoctorId)
                && appointment.getServiceDate().equals(targetDate)) {
            throw new ConflictException("Choose a different doctor or service date.");
        }
        Doctor target = doctors.findByIdForUpdate(targetDoctorId)
                .orElseThrow(() -> new NotFoundException("Target doctor was not found."));
        DailySchedule daily = requireBookableDay(target, targetDate);
        DoctorDayLedger targetLedger = ledgers.findForUpdate(target.getId(), targetDate)
                .orElseGet(() -> ledgers.save(new DoctorDayLedger(target, targetDate, daily.capacity())));
        Integer targetPosition = targetLedger.allocate();
        if (targetPosition == null) {
            throw new ConflictException("The selected recovery date has no guaranteed position. Choose another date.");
        }
        Doctor originalDoctor = appointment.getDoctor();
        LocalDate originalDate = appointment.getServiceDate();
        releaseAndPromote(appointment);
        appointment.reschedule(target, targetDate, targetPosition, clock.instant());
        audit.record("APPOINTMENT_RECOVERY_RESCHEDULED", "APPOINTMENT", appointment.getId(),
                appointment.getHospital().getId());
        notifications.notifyAppointment(appointment, NotificationType.APPOINTMENT_RESCHEDULED,
                "recovery:" + originalDoctor.getId() + ":" + originalDate,
                "Appointment rescheduled with your approval", "Your new OPD date is " + targetDate
                        + " with " + target.getName() + ", position " + targetPosition + ".");
    }

    @Transactional
    public void cancelForDoctorUnavailability(UUID appointmentId) {
        Appointment appointment = requireAppointmentForUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new ConflictException("This appointment cannot enter doctor-unavailability refund review.");
        }
        releaseAndPromote(appointment);
        appointment.cancel(clock.instant(), "Doctor unavailable; patient selected refund review");
        audit.record("APPOINTMENT_CANCELLED_DOCTOR_UNAVAILABLE", "APPOINTMENT", appointment.getId(),
                appointment.getHospital().getId());
        notifications.notifyAppointment(appointment, NotificationType.REFUND_REVIEW_REQUIRED,
                "doctor-unavailable", "Refund review requested",
                "The appointment is cancelled and awaits the authorized payment/refund workflow.");
    }

    @Transactional
    public void markNoShowOperational(UUID appointmentId) {
        Appointment appointment = requireAppointmentForUpdate(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only a confirmed appointment can be marked no-show.");
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(appointment.getHospital().getTimeZone())));
        if (appointment.getServiceDate().isAfter(today)) {
            throw new ConflictException("A future appointment cannot be marked no-show.");
        }
        DailySchedule daily = dailySchedule(appointment.getDoctor(), appointment.getServiceDate());
        Instant eligibleAt = LocalDateTime.of(appointment.getServiceDate(), daily.start())
                .atZone(ZoneId.of(appointment.getHospital().getTimeZone())).toInstant()
                .plus(properties.noShowGracePeriod());
        if (clock.instant().isBefore(eligibleAt)) {
            throw new ConflictException("The patient can be marked no-show only after the 15-minute grace period.");
        }
        releaseAndPromote(appointment);
        appointment.markNoShow(clock.instant());
        audit.record("APPOINTMENT_MARKED_NO_SHOW", "APPOINTMENT", appointment.getId(),
                appointment.getHospital().getId());
        notifications.notifyAppointment(appointment, NotificationType.NO_SHOW, "staff-marked",
                "Appointment marked no-show",
                "Hospital staff recorded that check-in was not completed for this appointment.");
    }

    @Scheduled(fixedDelayString = "${smartcare.queue.expiry-scan-ms:60000}",
            initialDelayString = "${smartcare.queue.expiry-initial-delay-ms:60000}")
    @Transactional
    public void expireDueReservations() {
        Instant now = clock.instant();
        List<Appointment> due = new java.util.ArrayList<>();
        due.addAll(appointments.findAllByStatusAndReservationExpiresAtBefore(
                AppointmentStatus.RESERVED_PENDING_PAYMENT, now));
        due.addAll(appointments.findAllByStatusAndCashDeadlineAtBefore(AppointmentStatus.CASH_PENDING, now));
        for (Appointment appointment : due) {
            doctors.findByIdForUpdate(appointment.getDoctor().getId()).ifPresent(ignored -> {
                releaseAndPromote(appointment);
                appointment.expire();
                audit.recordAs("SYSTEM", "APPOINTMENT_EXPIRED", "APPOINTMENT", appointment.getId(),
                        appointment.getHospital().getId());
                notifications.notifyAppointment(appointment, NotificationType.RESERVATION_EXPIRED, "expired",
                        "Reservation expired", "The payment or cash-confirmation deadline passed and the OPD position was released.");
            });
        }
    }

    private void releaseAndPromote(Appointment appointment) {
        DoctorDayLedger ledger = ledgers.findForUpdate(appointment.getDoctor().getId(), appointment.getServiceDate())
                .orElseThrow(() -> new IllegalStateException("Queue ledger is missing for an allocated appointment."));
        ledger.release();
        waitlist.findFirstByDoctorIdAndServiceDateAndStatusOrderBySequenceNumberAsc(
                        appointment.getDoctor().getId(), appointment.getServiceDate(), WaitlistStatus.WAITING)
                .ifPresent(entry -> {
                    Integer promotedPosition = ledger.allocate();
                    if (promotedPosition == null) return;
                    Appointment promoted = entry.getAppointment();
                    DailySchedule daily = dailySchedule(promoted.getDoctor(), promoted.getServiceDate());
                    Instant now = clock.instant();
                    promoted.reserve(promotedPosition, now.plus(properties.onlineReservationTtl()),
                            cashDeadline(promoted.getDoctor(), promoted.getServiceDate(), daily.start(), now));
                    entry.promote(now);
                    audit.recordAs("SYSTEM", "WAITLIST_PROMOTED", "APPOINTMENT", promoted.getId(),
                            promoted.getHospital().getId());
                    notifications.notifyAppointment(promoted, NotificationType.WAITLIST_PROMOTED, "promoted",
                            "A queue position is available", "You have been promoted from the waitlist to OPD number "
                                    + promotedPosition + ". Complete the required payment confirmation in time.");
                });
    }

    private DailySchedule requireBookableDay(Doctor doctor, LocalDate date) {
        if (!doctor.isActive()) throw new ConflictException("This doctor is not accepting bookings.");
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(doctor.getHospital().getTimeZone())));
        if (date.isBefore(today)) throw new IllegalArgumentException("The visit date cannot be in the past.");
        DailySchedule schedule = dailySchedule(doctor, date);
        if (date.equals(today)) {
            LocalTime now = LocalTime.now(clock.withZone(ZoneId.of(doctor.getHospital().getTimeZone())));
            if (!schedule.end().isAfter(now)) throw new ConflictException("Today's OPD schedule has ended.");
        }
        return schedule;
    }

    private DailySchedule dailySchedule(Doctor doctor, LocalDate date) {
        List<DoctorSchedule> day = schedules.findAllByDoctorIdAndDayOfWeek(doctor.getId(), date.getDayOfWeek());
        if (day.isEmpty()) throw new ConflictException("The doctor has no OPD schedule on this date.");
        int capacity = day.stream().map(DoctorSchedule::getCapacityOverride).filter(java.util.Objects::nonNull)
                .findFirst().orElse(doctor.getDailyMaxCapacity());
        LocalTime start = day.stream().map(DoctorSchedule::getStartTime).min(LocalTime::compareTo).orElseThrow();
        LocalTime end = day.stream().map(DoctorSchedule::getEndTime).max(LocalTime::compareTo).orElseThrow();
        return new DailySchedule(capacity, start, end);
    }

    private Instant cashDeadline(Doctor doctor, LocalDate date, LocalTime start, Instant now) {
        ZoneId zone = ZoneId.of(doctor.getHospital().getTimeZone());
        Instant planned = LocalDateTime.of(date, start).atZone(zone).toInstant().minusSeconds(30 * 60L);
        return planned.isAfter(now) ? planned : now.plus(properties.cashGracePeriod());
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required for this operation."));
    }

    private Appointment requireAppointment(UUID id) {
        return appointments.findById(id).orElseThrow(() -> new NotFoundException("Appointment was not found."));
    }

    private Appointment requireAppointmentForUpdate(UUID id) {
        return appointments.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        Doctor doctor = appointment.getDoctor();
        int estimated = appointment.getQueuePosition() == null ? 0
                : Math.max(0, appointment.getQueuePosition() - 1) * doctor.getExpectedConsultationMinutes();
        return new AppointmentResponse(appointment.getId(), appointment.getPatient().getPatientNumber(),
                doctor.getId(), doctor.getName(),
                doctor.getSpecialization(), appointment.getHospital().getId(), appointment.getHospital().getName(),
                doctor.getDepartment().getName(), appointment.getServiceDate(), appointment.getQueuePosition(),
                appointment.getStatus(), appointment.getPaymentMethod(), appointment.getAmount(),
                appointment.getReservationExpiresAt(), appointment.getCashDeadlineAt(), estimated,
                doctor.getBuilding(), doctor.getFloorLabel(), doctor.getRoomNumber(), appointment.getCheckedInAt(),
                appointment.getConsultationStartedAt(), appointment.getCompletedAt(), appointment.getCreatedAt());
    }

    private void notifyConfirmed(Appointment appointment, String source) {
        notifications.notifyAppointment(appointment, NotificationType.PAYMENT_SUCCESSFUL, source,
                "Payment verified", "Your consultation fee has been verified by the configured payment workflow.");
        notifications.notifyAppointment(appointment, NotificationType.APPOINTMENT_CONFIRMED, source,
                "Appointment confirmed", "Your OPD number " + appointment.getQueuePosition() + " is confirmed.");
    }

    private record DailySchedule(int capacity, LocalTime start, LocalTime end) {
    }
}
