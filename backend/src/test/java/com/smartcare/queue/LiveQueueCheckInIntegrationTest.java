package com.smartcare.queue;

import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.domain.PaymentMethod;
import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.appointment.web.AppointmentDtos.BookingRequest;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.AuthResponse;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.checkin.domain.CheckInChannel;
import com.smartcare.checkin.service.CheckInService;
import com.smartcare.doctor.service.DoctorService;
import com.smartcare.doctor.web.DoctorDtos.DoctorRequest;
import com.smartcare.doctor.web.DoctorDtos.ScheduleRequest;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.notification.service.NotificationService;
import com.smartcare.payment.service.PaymentService;
import com.smartcare.queue.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LiveQueueCheckInIntegrationTest {

    @Autowired AppointmentService appointments;
    @Autowired PaymentService payments;
    @Autowired CheckInService checkIns;
    @Autowired QueueService queues;
    @Autowired NotificationService notifications;
    @Autowired AuthService auth;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;

    @Test
    @WithMockUser(roles = {"CASHIER", "RECEPTIONIST"})
    void patientCheckInAndPrivacySafeLiveQueueAdvanceAreDurable() {
        LocalDate visitDate = LocalDate.now();
        var hospital = hospitals.create(new HospitalRequest("SC-LIVE-1", "Live Queue Test Hospital",
                "4 Queue Road", "Delhi", "Delhi", "110001", "+911112345688", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("OPD", "General OPD", "Live queue test department"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Queue Test",
                "Internal Medicine", "SC-LIVE-REG-1", new BigDecimal("500.00"), 15, 10,
                "Queue Block", "1st Floor", "OPD 11", true));
        doctors.addSchedule(doctor.id(), new ScheduleRequest(visitDate.getDayOfWeek(),
                LocalTime.of(0, 1), LocalTime.of(23, 59), 15, 10));
        AuthResponse first = patient("21", "first");
        AuthResponse second = patient("22", "second");

        var firstAppointment = appointments.book(first.user().id(), "live-booking-1",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));
        var secondAppointment = appointments.book(second.user().id(), "live-booking-2",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));
        payments.confirmCash(firstAppointment.id());
        payments.confirmCash(secondAppointment.id());

        var firstCheckIn = checkIns.checkIn(first.user().id(), List.of("PATIENT"), firstAppointment.id(),
                CheckInChannel.MOBILE_WEB);
        var repeated = checkIns.checkIn(first.user().id(), List.of("PATIENT"), firstAppointment.id(),
                CheckInChannel.MOBILE_WEB);
        var secondCheckIn = checkIns.checkIn(second.user().id(), List.of("PATIENT"), secondAppointment.id(),
                CheckInChannel.QR_CODE);
        assertThat(repeated.id()).isEqualTo(firstCheckIn.id());
        assertThat(firstCheckIn.privacyToken()).startsWith("RVQ-");
        assertThat(secondCheckIn.appointmentStatus()).isEqualTo(AppointmentStatus.CHECKED_IN);

        var publicBefore = queues.publicSnapshot(doctor.id(), visitDate);
        assertThat(publicBefore.currentlyServingPosition()).isNull();
        assertThat(publicBefore.checkedInWaiting()).isEqualTo(2);

        var servingFirst = queues.serveNext(doctor.id(), visitDate);
        assertThat(servingFirst.currentlyServingPosition()).isEqualTo(1);
        assertThat(servingFirst.currentlyServingToken()).startsWith("RVQ-");
        assertThat(servingFirst.toString()).doesNotContain(first.user().displayName());

        var secondQueue = queues.patientSnapshot(second.user().id(), secondAppointment.id());
        assertThat(secondQueue.patientsAhead()).isEqualTo(1);
        assertThat(secondQueue.estimatedWaitMinutes()).isEqualTo(15);
        assertThat(notifications.unreadCount(second.user().id())).isGreaterThan(0);

        var servingSecond = queues.serveNext(doctor.id(), visitDate);
        assertThat(servingSecond.currentlyServingPosition()).isEqualTo(2);
        assertThat(queues.patientSnapshot(first.user().id(), firstAppointment.id()).status())
                .isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(queues.patientSnapshot(second.user().id(), secondAppointment.id()).status())
                .isEqualTo(AppointmentStatus.IN_CONSULTATION);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9190000000" + digits, "live." + label + "@example.com",
                "Live Patient " + label, "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
