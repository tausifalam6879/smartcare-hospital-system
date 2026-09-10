package com.smartcare.queue.service;

import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.repository.AppointmentRepository;
import com.smartcare.audit.service.AuditService;
import com.smartcare.checkin.repository.CheckInRepository;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import com.smartcare.operations.repository.DoctorDayOperationRepository;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import com.smartcare.queue.web.QueueDtos.PatientQueueResponse;
import com.smartcare.queue.web.QueueDtos.PublicQueueResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class QueueService {

    private static final EnumSet<AppointmentStatus> LIVE_STATUSES = EnumSet.of(
            AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_CONSULTATION);
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final CheckInRepository checkIns;
    private final NotificationService notifications;
    private final DoctorDayOperationRepository doctorDayOperations;
    private final AuditService audit;
    private final Clock clock;

    public QueueService(AppointmentRepository appointments, DoctorRepository doctors, PatientRepository patients,
                        CheckInRepository checkIns, NotificationService notifications,
                        DoctorDayOperationRepository doctorDayOperations, AuditService audit, Clock clock) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.patients = patients;
        this.checkIns = checkIns;
        this.notifications = notifications;
        this.doctorDayOperations = doctorDayOperations;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PublicQueueResponse publicSnapshot(UUID doctorId, LocalDate date) {
        Doctor doctor = requireDoctor(doctorId);
        return publicSnapshot(doctor, date, liveAppointments(doctorId, date));
    }

    @Transactional(readOnly = true)
    public PatientQueueResponse patientSnapshot(UUID userId, UUID appointmentId) {
        Patient patient = requirePatient(userId);
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Appointment belongs to another patient.");
        }
        List<Appointment> live = liveAppointments(appointment.getDoctor().getId(), appointment.getServiceDate());
        return patientSnapshot(appointment, live);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public PublicQueueResponse serveNext(UUID doctorId, LocalDate date) {
        Doctor doctor = doctors.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor was not found."));
        Appointment current = appointments.findFirstByDoctorIdAndServiceDateAndStatusOrderByQueuePositionAsc(
                doctorId, date, AppointmentStatus.IN_CONSULTATION).orElse(null);
        Instant now = clock.instant();
        if (current != null) {
            current.complete(now);
            notifications.notifyAppointment(current, NotificationType.VISIT_COMPLETED, "completed",
                    "Consultation completed", "Your SmartCare queue visit has been marked complete.");
        }
        Appointment next = appointments.findFirstByDoctorIdAndServiceDateAndStatusOrderByQueuePositionAsc(
                doctorId, date, AppointmentStatus.CHECKED_IN).orElse(null);
        if (next != null) {
            next.startConsultation(now);
            notifications.notifyAppointment(next, NotificationType.NOW_SERVING, "now-serving",
                    "It is your turn", "Please proceed to " + roomLabel(next.getDoctor()) + ".");
            audit.record("QUEUE_CONSULTATION_STARTED", "APPOINTMENT", next.getId(), next.getHospital().getId());
        } else if (current == null) {
            throw new ConflictException("No checked-in patient is waiting for this queue.");
        }
        List<Appointment> live = liveAppointments(doctorId, date);
        Integer servingPosition = next == null ? null : next.getQueuePosition();
        for (Appointment appointment : live) {
            if (appointment.getStatus() == AppointmentStatus.IN_CONSULTATION) continue;
            PatientQueueResponse snapshot = patientSnapshot(appointment, live);
            notifications.notifyAppointment(appointment, NotificationType.QUEUE_POSITION_UPDATED,
                    "serving-" + (servingPosition == null ? "none" : servingPosition),
                    "Queue position updated", snapshot.patientsAhead() + " patient(s) are approximately ahead of you."
                            + " Estimated wait: ~" + snapshot.estimatedWaitMinutes() + " minutes.");
        }
        return publicSnapshot(doctor, date, live);
    }

    private PublicQueueResponse publicSnapshot(Doctor doctor, LocalDate date, List<Appointment> live) {
        Appointment current = live.stream().filter(item -> item.getStatus() == AppointmentStatus.IN_CONSULTATION)
                .findFirst().orElse(null);
        String token = current == null ? null : checkIns.findByAppointmentId(current.getId())
                .map(item -> item.getPrivacyToken()).orElse(queueToken(current.getQueuePosition()));
        int waiting = (int) live.stream().filter(item -> item.getStatus() == AppointmentStatus.CHECKED_IN).count();
        Instant updated = live.stream().map(Appointment::getUpdatedAt).filter(java.util.Objects::nonNull)
                .max(Instant::compareTo).orElse(clock.instant());
        return new PublicQueueResponse(doctor.getId(), doctor.getName(), doctor.getHospital().getId(),
                doctor.getHospital().getName(), date, current == null ? null : current.getQueuePosition(), token,
                waiting, 15, updated);
    }

    private PatientQueueResponse patientSnapshot(Appointment appointment, List<Appointment> live) {
        int ahead = appointment.getQueuePosition() == null ? 0 : (int) live.stream()
                .filter(item -> item.getQueuePosition() != null
                        && item.getQueuePosition() < appointment.getQueuePosition()
                        && item.getStatus() != AppointmentStatus.COMPLETED)
                .count();
        if (appointment.getStatus() == AppointmentStatus.IN_CONSULTATION) ahead = 0;
        Appointment current = live.stream().filter(item -> item.getStatus() == AppointmentStatus.IN_CONSULTATION)
                .findFirst().orElse(null);
        String privacyToken = checkIns.findByAppointmentId(appointment.getId())
                .map(item -> item.getPrivacyToken()).orElse(null);
        Doctor doctor = appointment.getDoctor();
        int operationalDelay = doctorDayOperations
                .findByDoctorIdAndServiceDate(doctor.getId(), appointment.getServiceDate())
                .map(operation -> operation.getDelayMinutes())
                .orElse(0);
        String notice = operationalDelay > 0
                ? "Includes a reported doctor delay of about " + operationalDelay
                + " minutes. This remains an approximate estimate; emergencies and clinical priorities may change it."
                : "Approximate estimate only; emergencies and clinical priorities may change the queue.";
        return new PatientQueueResponse(appointment.getId(), doctor.getId(), doctor.getName(),
                appointment.getHospital().getName(), appointment.getServiceDate(), appointment.getStatus(),
                appointment.getQueuePosition(), privacyToken, current == null ? null : current.getQueuePosition(),
                ahead, ahead * doctor.getExpectedConsultationMinutes() + operationalDelay, notice,
                doctor.getBuilding(), doctor.getFloorLabel(), doctor.getRoomNumber(), clock.instant());
    }

    private List<Appointment> liveAppointments(UUID doctorId, LocalDate date) {
        return appointments.findAllByDoctorIdAndServiceDateAndStatusInOrderByQueuePositionAsc(
                doctorId, date, LIVE_STATUSES);
    }

    private Doctor requireDoctor(UUID id) {
        return doctors.findById(id).orElseThrow(() -> new NotFoundException("Doctor was not found."));
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required for this operation."));
    }

    private static String queueToken(Integer position) {
        return position == null ? null : "OPD-" + String.format("%03d", position);
    }

    private static String roomLabel(Doctor doctor) {
        return java.util.stream.Stream.of(doctor.getBuilding(), doctor.getFloorLabel(), doctor.getRoomNumber())
                .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining(", "));
    }
}
