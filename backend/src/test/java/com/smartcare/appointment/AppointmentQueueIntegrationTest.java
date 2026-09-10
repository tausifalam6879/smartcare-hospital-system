package com.smartcare.appointment;

import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.domain.PaymentMethod;
import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.appointment.web.AppointmentDtos.BookingRequest;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.doctor.service.DoctorService;
import com.smartcare.doctor.web.DoctorDtos.DoctorRequest;
import com.smartcare.doctor.web.DoctorDtos.ScheduleRequest;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AppointmentQueueIntegrationTest {

    @Autowired AppointmentService appointments;
    @Autowired AuthService auth;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;

    @Test
    void capacityIdempotencyWaitlistAndPromotionRemainFair() {
        var hospital = hospitals.create(new HospitalRequest("SC-QTEST", "Queue Test Hospital",
                "1 Test Road", "Delhi", "Delhi", "110001", "+911112345678", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("MED", "General Medicine", "Test department"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Queue Test",
                "Internal Medicine", "SC-Q-001", new BigDecimal("500.00"), 15, 1,
                "Block A", "1st Floor", "OPD 1", true));
        LocalDate visitDate = LocalDate.now().plusDays(2);
        doctors.addSchedule(doctor.id(), new ScheduleRequest(visitDate.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(13, 0), 15, 1));

        var firstPatient = register("+919000000001", "first.queue@example.com", "First Patient");
        var secondPatient = register("+919000000002", "second.queue@example.com", "Second Patient");

        BookingRequest online = new BookingRequest(doctor.id(), visitDate, PaymentMethod.ONLINE);
        var first = appointments.book(firstPatient.user().id(), "same-request", online);
        var repeated = appointments.book(firstPatient.user().id(), "same-request", online);
        assertThat(first.status()).isEqualTo(AppointmentStatus.RESERVED_PENDING_PAYMENT);
        assertThat(first.queuePosition()).isEqualTo(1);
        assertThat(repeated.id()).isEqualTo(first.id());

        var second = appointments.book(secondPatient.user().id(), "second-request",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));
        assertThat(second.status()).isEqualTo(AppointmentStatus.WAITLISTED);
        assertThat(second.queuePosition()).isNull();

        appointments.cancel(firstPatient.user().id(), first.id(), "Plans changed");
        var promoted = appointments.mine(secondPatient.user().id()).get(0);
        assertThat(promoted.status()).isEqualTo(AppointmentStatus.CASH_PENDING);
        assertThat(promoted.queuePosition()).isEqualTo(2);
        assertThat(appointments.availability(doctor.id(), visitDate).waitlistCount()).isZero();
        assertThat(appointments.availability(doctor.id(), visitDate).positionsAvailable()).isZero();
    }

    @Test
    void noShowReleasesCapacityAndPromotesTheFirstWaitlistedPatient() {
        var hospital = hospitals.create(new HospitalRequest("SC-NSHOW", "No-show Test Hospital",
                "2 Test Road", "Delhi", "Delhi", "110002", "+911112345679", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("NS-MED", "No-show Medicine", "No-show workflow test"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. No-show Test",
                "Internal Medicine", "SC-NS-001", new BigDecimal("500.00"), 15, 1,
                "Block N", "1st Floor", "OPD 2", true));
        LocalDate visitDate = LocalDate.now();
        doctors.addSchedule(doctor.id(), new ScheduleRequest(visitDate.getDayOfWeek(),
                LocalTime.MIDNIGHT, LocalTime.of(23, 59), 15, 1));

        var firstPatient = register("+919000000011", "first.noshow@example.com", "First No-show Patient");
        var waitingPatient = register("+919000000012", "waiting.noshow@example.com", "Waiting Patient");
        var first = appointments.book(firstPatient.user().id(), "no-show-first",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));
        appointments.confirmCash(first.id());
        var waiting = appointments.book(waitingPatient.user().id(), "no-show-waiting",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));
        assertThat(waiting.status()).isEqualTo(AppointmentStatus.WAITLISTED);

        appointments.markNoShowOperational(first.id());

        assertThat(appointments.mine(firstPatient.user().id()).get(0).status())
                .isEqualTo(AppointmentStatus.NO_SHOW);
        var promoted = appointments.mine(waitingPatient.user().id()).get(0);
        assertThat(promoted.status()).isEqualTo(AppointmentStatus.CASH_PENDING);
        assertThat(promoted.queuePosition()).isEqualTo(2);
        assertThat(appointments.availability(doctor.id(), visitDate).waitlistCount()).isZero();
    }

    private com.smartcare.auth.web.AuthResponse register(String mobile, String email, String name) {
        return auth.register(new RegisterRequest(mobile, email, name, "safe-test-password",
                LocalDate.of(1990, 1, 1), null, null, null, "en"));
    }
}
