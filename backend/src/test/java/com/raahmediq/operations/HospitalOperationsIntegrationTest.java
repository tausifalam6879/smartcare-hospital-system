package com.raahmediq.operations;

import com.raahmediq.appointment.domain.AppointmentStatus;
import com.raahmediq.appointment.domain.PaymentMethod;
import com.raahmediq.appointment.repository.AppointmentRepository;
import com.raahmediq.appointment.service.AppointmentService;
import com.raahmediq.appointment.web.AppointmentDtos.BookingRequest;
import com.raahmediq.auth.domain.Role;
import com.raahmediq.auth.repository.UserAccountRepository;
import com.raahmediq.auth.service.AuthService;
import com.raahmediq.auth.web.AuthResponse;
import com.raahmediq.auth.web.RegisterRequest;
import com.raahmediq.doctor.service.DoctorService;
import com.raahmediq.doctor.web.DoctorDtos.DoctorRequest;
import com.raahmediq.doctor.web.DoctorDtos.ScheduleRequest;
import com.raahmediq.hospital.service.HospitalService;
import com.raahmediq.hospital.web.HospitalDtos.DepartmentRequest;
import com.raahmediq.hospital.web.HospitalDtos.HospitalRequest;
import com.raahmediq.notification.domain.NotificationType;
import com.raahmediq.notification.repository.NotificationRepository;
import com.raahmediq.operations.domain.DoctorDayStatus;
import com.raahmediq.operations.domain.RecoveryChoice;
import com.raahmediq.operations.domain.RecoveryStatus;
import com.raahmediq.operations.service.HospitalOperationsService;
import com.raahmediq.operations.web.OperationsDtos.ResolveRecovery;
import com.raahmediq.operations.web.OperationsDtos.UpdateDoctorDayStatus;
import com.raahmediq.queue.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class HospitalOperationsIntegrationTest {

    @Autowired HospitalOperationsService operations;
    @Autowired AppointmentService appointments;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired QueueService queues;
    @Autowired AuthService auth;
    @Autowired UserAccountRepository users;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;
    @Autowired NotificationRepository notifications;

    @Test
    @WithMockUser(roles = {"HOSPITAL_ADMIN"})
    void cancellationRequiresPatientChoiceAndApprovedReschedulePreservesConfirmedBooking() {
        var hospital = hospitals.create(new HospitalRequest("RMQ-OPS-1", "Operations Test Hospital",
                "11 Flow Road", "Delhi", "Delhi", "110001", "+911140404111", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("OPS-MED", "Operations Medicine", "Operations test department"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Flow Control",
                "Internal Medicine", "RMQ-OPS-DOC-1", new BigDecimal("700.00"), 15, 5,
                "Block O", "2nd Floor", "OPD 11", true));
        LocalDate originalDate = LocalDate.now().plusDays(2);
        LocalDate targetDate = originalDate.plusDays(7);
        doctors.addSchedule(doctor.id(), new ScheduleRequest(originalDate.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(13, 0), 15, 5));

        AuthResponse patient = patient("81", "operations.patient");
        AuthResponse otherPatient = patient("82", "operations.other");
        AuthResponse admin = patient("83", "operations.admin");
        users.findById(admin.user().id()).orElseThrow().grantRole(Role.HOSPITAL_ADMIN);

        var booked = appointments.book(patient.user().id(), "ops-booking",
                new BookingRequest(doctor.id(), originalDate, PaymentMethod.CASH));
        appointments.confirmCash(booked.id());

        operations.updateDoctorStatus(admin.user().id(), new UpdateDoctorDayStatus(
                doctor.id(), originalDate, DoctorDayStatus.CANCELLED_FOR_DAY,
                "Doctor assigned to an emergency procedure."));

        var unchanged = appointmentRepository.findById(booked.id()).orElseThrow();
        assertThat(unchanged.getDoctor().getId()).isEqualTo(doctor.id());
        assertThat(unchanged.getServiceDate()).isEqualTo(originalDate);
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        var recovery = operations.recoveryMine(patient.user().id()).get(0);
        assertThat(recovery.status()).isEqualTo(RecoveryStatus.AWAITING_PATIENT_CHOICE);
        assertThat(recovery.originalQueuePosition()).isEqualTo(1);
        assertThatThrownBy(() -> operations.resolveRecovery(otherPatient.user().id(), recovery.id(),
                new ResolveRecovery(RecoveryChoice.RESCHEDULE_SAME_DOCTOR, null, targetDate)))
                .isInstanceOf(AccessDeniedException.class);

        var resolved = operations.resolveRecovery(patient.user().id(), recovery.id(),
                new ResolveRecovery(RecoveryChoice.RESCHEDULE_SAME_DOCTOR, null, targetDate));
        assertThat(resolved.status()).isEqualTo(RecoveryStatus.RESCHEDULED);
        assertThat(resolved.targetDate()).isEqualTo(targetDate);
        var moved = appointmentRepository.findById(booked.id()).orElseThrow();
        assertThat(moved.getDoctor().getId()).isEqualTo(doctor.id());
        assertThat(moved.getServiceDate()).isEqualTo(targetDate);
        assertThat(moved.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        operations.updateDoctorStatus(admin.user().id(), new UpdateDoctorDayStatus(
                doctor.id(), targetDate, DoctorDayStatus.DELAYED_30, "Traffic delay reported by care desk."));
        assertThat(queues.patientSnapshot(patient.user().id(), booked.id()).estimatedWaitMinutes())
                .isGreaterThanOrEqualTo(30);
        var dashboard = operations.dashboard(admin.user().id(), hospital.id(), targetDate);
        assertThat(dashboard.totalAppointments()).isEqualTo(1);
        assertThat(dashboard.confirmed()).isEqualTo(1);
        assertThat(dashboard.delayedOrUnavailableDoctors()).isEqualTo(1);
        assertThat(notifications.findAll()).extracting(item -> item.getType()).contains(
                NotificationType.DOCTOR_UNAVAILABLE, NotificationType.APPOINTMENT_RECOVERY_REQUIRED,
                NotificationType.APPOINTMENT_RESCHEDULED, NotificationType.DOCTOR_DELAYED);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9194000000" + digits, label + "@example.com",
                "Operations Workflow User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
