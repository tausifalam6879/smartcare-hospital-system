package com.smartcare.checkin.service;

import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.repository.AppointmentRepository;
import com.smartcare.audit.service.AuditService;
import com.smartcare.checkin.config.CheckInProperties;
import com.smartcare.checkin.domain.CheckIn;
import com.smartcare.checkin.domain.CheckInChannel;
import com.smartcare.checkin.repository.CheckInRepository;
import com.smartcare.checkin.web.CheckInDtos.CheckInResponse;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.doctor.domain.DoctorSchedule;
import com.smartcare.doctor.repository.DoctorScheduleRepository;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CheckInService {

    private static final Set<String> STAFF_ROLES = Set.of("DOCTOR", "RECEPTIONIST", "HOSPITAL_ADMIN", "SUPER_ADMIN");
    private final CheckInRepository checkIns;
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorScheduleRepository schedules;
    private final CheckInProperties properties;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public CheckInService(CheckInRepository checkIns, AppointmentRepository appointments, PatientRepository patients,
                          DoctorScheduleRepository schedules, CheckInProperties properties,
                          NotificationService notifications, AuditService audit, Clock clock) {
        this.checkIns = checkIns;
        this.appointments = appointments;
        this.patients = patients;
        this.schedules = schedules;
        this.properties = properties;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public CheckInResponse checkIn(UUID actorUserId, Collection<String> actorRoles, UUID appointmentId,
                                   CheckInChannel channel) {
        CheckIn existing = checkIns.findByAppointmentId(appointmentId).orElse(null);
        if (existing != null) {
            authorize(actorUserId, actorRoles, existing.getAppointment(), channel, false);
            return toResponse(existing);
        }
        Appointment appointment = appointments.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
        existing = checkIns.findByAppointmentId(appointmentId).orElse(null);
        if (existing != null) {
            authorize(actorUserId, actorRoles, existing.getAppointment(), channel, false);
            return toResponse(existing);
        }
        authorize(actorUserId, actorRoles, appointment, channel, true);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException("Only a confirmed appointment can be checked in.");
        }
        validateWindow(appointment);
        Instant now = clock.instant();
        appointment.checkIn(now);
        String privacyToken = "RVQ-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        CheckIn checkIn = checkIns.save(new CheckIn(appointment, channel, privacyToken, now, actorUserId));
        notifications.notifyAppointment(appointment, NotificationType.CHECK_IN_CONFIRMED, "checked-in",
                "Check-in complete", "Your private queue token is " + privacyToken
                        + ". Live wait time remains an estimate.");
        audit.record("PATIENT_CHECKED_IN", "CHECK_IN", checkIn.getId(), appointment.getHospital().getId());
        return toResponse(checkIn);
    }

    @Transactional(readOnly = true)
    public CheckInResponse get(UUID actorUserId, Collection<String> actorRoles, UUID appointmentId) {
        CheckIn checkIn = checkIns.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Check-in was not found."));
        authorize(actorUserId, actorRoles, checkIn.getAppointment(), checkIn.getChannel(), false);
        return toResponse(checkIn);
    }

    private void authorize(UUID actorUserId, Collection<String> roles, Appointment appointment,
                           CheckInChannel channel, boolean validateChannel) {
        boolean staff = roles != null && roles.stream().anyMatch(STAFF_ROLES::contains);
        Patient actorPatient = patients.findByUserId(actorUserId).orElse(null);
        boolean owner = actorPatient != null && appointment.getPatient().getId().equals(actorPatient.getId());
        if (!staff && !owner) throw new AccessDeniedException("You cannot check in this appointment.");
        if (validateChannel && !staff && channel != CheckInChannel.MOBILE_WEB && channel != CheckInChannel.QR_CODE) {
            throw new AccessDeniedException("Reception and kiosk check-in must be verified by hospital staff.");
        }
    }

    private void validateWindow(Appointment appointment) {
        ZoneId zone = ZoneId.of(appointment.getHospital().getTimeZone());
        List<DoctorSchedule> day = schedules.findAllByDoctorIdAndDayOfWeek(
                appointment.getDoctor().getId(), appointment.getServiceDate().getDayOfWeek());
        if (day.isEmpty()) throw new ConflictException("The doctor schedule is unavailable for check-in.");
        LocalTime start = day.stream().map(DoctorSchedule::getStartTime).min(LocalTime::compareTo).orElseThrow();
        LocalTime end = day.stream().map(DoctorSchedule::getEndTime).max(LocalTime::compareTo).orElseThrow();
        Instant opensAt = LocalDateTime.of(appointment.getServiceDate(), start).atZone(zone).toInstant()
                .minus(properties.opensBefore());
        Instant closesAt = LocalDateTime.of(appointment.getServiceDate(), end).atZone(zone).toInstant()
                .plus(properties.closesAfter());
        Instant now = clock.instant();
        if (now.isBefore(opensAt)) throw new ConflictException("Check-in is not open yet.");
        if (now.isAfter(closesAt)) throw new ConflictException("The check-in window has closed.");
    }

    private CheckInResponse toResponse(CheckIn checkIn) {
        Appointment appointment = checkIn.getAppointment();
        return new CheckInResponse(checkIn.getId(), appointment.getId(), checkIn.getChannel(),
                checkIn.getPrivacyToken(), appointment.getQueuePosition(), appointment.getStatus(),
                checkIn.getCheckedInAt());
    }
}
