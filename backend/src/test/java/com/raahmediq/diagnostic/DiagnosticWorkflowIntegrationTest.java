package com.raahmediq.diagnostic;

import com.raahmediq.appointment.domain.PaymentMethod;
import com.raahmediq.appointment.service.AppointmentService;
import com.raahmediq.appointment.web.AppointmentDtos.BookingRequest;
import com.raahmediq.audit.repository.AuditLogRepository;
import com.raahmediq.auth.domain.Role;
import com.raahmediq.auth.repository.UserAccountRepository;
import com.raahmediq.auth.service.AuthService;
import com.raahmediq.auth.web.AuthResponse;
import com.raahmediq.auth.web.RegisterRequest;
import com.raahmediq.checkin.domain.CheckInChannel;
import com.raahmediq.checkin.service.CheckInService;
import com.raahmediq.common.error.ConflictException;
import com.raahmediq.diagnostic.domain.DiagnosticModality;
import com.raahmediq.diagnostic.domain.DiagnosticOrderStatus;
import com.raahmediq.diagnostic.domain.DiagnosticPriority;
import com.raahmediq.diagnostic.domain.DiagnosticResultFlag;
import com.raahmediq.diagnostic.service.DiagnosticWorkflowService;
import com.raahmediq.diagnostic.web.DiagnosticDtos.CreateOrderRequest;
import com.raahmediq.diagnostic.web.DiagnosticDtos.ProcedureRequest;
import com.raahmediq.diagnostic.web.DiagnosticDtos.ResultItemRequest;
import com.raahmediq.diagnostic.web.DiagnosticDtos.ScheduleOrderRequest;
import com.raahmediq.diagnostic.web.DiagnosticDtos.VerifyResultRequest;
import com.raahmediq.doctor.service.DoctorService;
import com.raahmediq.doctor.web.DoctorDtos.DoctorRequest;
import com.raahmediq.doctor.web.DoctorDtos.LinkAccountRequest;
import com.raahmediq.doctor.web.DoctorDtos.ScheduleRequest;
import com.raahmediq.hospital.service.HospitalService;
import com.raahmediq.hospital.web.HospitalDtos.DepartmentRequest;
import com.raahmediq.hospital.web.HospitalDtos.HospitalRequest;
import com.raahmediq.payment.service.PaymentService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DiagnosticWorkflowIntegrationTest {

    @Autowired DiagnosticWorkflowService diagnostics;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;
    @Autowired AppointmentService appointments;
    @Autowired PaymentService payments;
    @Autowired CheckInService checkIns;
    @Autowired QueueService queues;
    @Autowired AuthService auth;
    @Autowired UserAccountRepository users;
    @Autowired AuditLogRepository auditLogs;

    @Test
    @WithMockUser(roles = {"HOSPITAL_ADMIN", "CASHIER", "RECEPTIONIST", "DOCTOR", "LAB_TECHNICIAN"})
    void diagnosticOrdersAreCapacitySafePatientIsolatedAndReleasedOnlyAfterVerification() {
        LocalDate today = LocalDate.now();
        LocalDate diagnosticDate = today.plusDays(1);
        var hospital = hospitals.create(new HospitalRequest("RMQ-DIAG-1", "Diagnostic Test Hospital",
                "8 Evidence Road", "Delhi", "Delhi", "110001", "+911112345688", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("MED", "Medicine", "Diagnostic workflow test"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Diagnostic Test",
                "Internal Medicine", "RMQ-DIAG-REG-1", new BigDecimal("700.00"), 15, 10,
                "Care Block", "1st Floor", "OPD 18", true));
        doctors.addSchedule(doctor.id(), new ScheduleRequest(today.getDayOfWeek(),
                LocalTime.of(0, 1), LocalTime.of(23, 59), 15, 10));

        var cbc = diagnostics.createProcedure(new ProcedureRequest(hospital.id(), "CBC", "Complete Blood Count",
                DiagnosticModality.LAB, "No fasting required. Bring your order.", 6, 1, 10,
                new BigDecimal("450.00"), "Diagnostics Block", "Ground Floor", "Lab 2"));

        AuthResponse owner = patient("51", "diagnostic.owner");
        AuthResponse other = patient("52", "diagnostic.other");
        AuthResponse doctorAccount = patient("53", "diagnostic.doctor");
        AuthResponse labAccount = patient("54", "diagnostic.lab");
        doctors.linkAccount(doctor.id(), new LinkAccountRequest(doctorAccount.user().id()));
        var labUser = users.findById(labAccount.user().id()).orElseThrow();
        labUser.grantRole(Role.LAB_TECHNICIAN);

        var firstAppointment = appointments.book(owner.user().id(), "diagnostic-appointment-1",
                new BookingRequest(doctor.id(), today, PaymentMethod.CASH));
        var secondAppointment = appointments.book(other.user().id(), "diagnostic-appointment-2",
                new BookingRequest(doctor.id(), today, PaymentMethod.CASH));
        payments.confirmCash(firstAppointment.id());
        payments.confirmCash(secondAppointment.id());
        checkIns.checkIn(owner.user().id(), List.of("PATIENT"), firstAppointment.id(), CheckInChannel.MOBILE_WEB);
        checkIns.checkIn(other.user().id(), List.of("PATIENT"), secondAppointment.id(), CheckInChannel.MOBILE_WEB);

        queues.serveNext(doctor.id(), today);
        var firstOrder = diagnostics.createOrder(doctorAccount.user().id(), new CreateOrderRequest(
                firstAppointment.id(), cbc.id(), DiagnosticPriority.ROUTINE, "Investigate persistent fatigue."));
        queues.serveNext(doctor.id(), today);
        var secondOrder = diagnostics.createOrder(doctorAccount.user().id(), new CreateOrderRequest(
                secondAppointment.id(), cbc.id(), DiagnosticPriority.ROUTINE, "Baseline blood count."));

        var scheduled = diagnostics.schedule(owner.user().id(), firstOrder.id(),
                new ScheduleOrderRequest(diagnosticDate));
        assertThat(scheduled.status()).isEqualTo(DiagnosticOrderStatus.SCHEDULED);
        assertThat(scheduled.queuePosition()).isEqualTo(1);
        assertThat(diagnostics.availability(cbc.id(), diagnosticDate).remaining()).isZero();
        assertThatThrownBy(() -> diagnostics.schedule(other.user().id(), secondOrder.id(),
                new ScheduleOrderRequest(diagnosticDate)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("fully booked");
        assertThatThrownBy(() -> diagnostics.cancel(other.user().id(), firstOrder.id(),
                new com.raahmediq.diagnostic.web.DiagnosticDtos.CancelOrderRequest(null)))
                .isInstanceOf(AccessDeniedException.class);

        diagnostics.collect(labAccount.user().id(), firstOrder.id());
        diagnostics.start(labAccount.user().id(), firstOrder.id());
        var verified = diagnostics.verifyResult(labAccount.user().id(), firstOrder.id(),
                new VerifyResultRequest("CBC completed and verified by diagnostic staff.",
                        "Haemoglobin 13.4 g/dL; total leukocyte count 7,200 /uL.",
                        "Values shown are within the supplied laboratory reference ranges.",
                        DiagnosticResultFlag.NORMAL,
                        List.of(new ResultItemRequest("Haemoglobin", "13.4", "g/dL", "12.0-16.0",
                                DiagnosticResultFlag.NORMAL))));

        assertThat(verified.status()).isEqualTo(DiagnosticOrderStatus.RESULT_VERIFIED);
        assertThat(verified.result()).isNotNull();
        assertThat(verified.result().items()).extracting(item -> item.name()).containsExactly("Haemoglobin");
        assertThat(diagnostics.mine(owner.user().id())).hasSize(1);
        assertThat(diagnostics.mine(other.user().id())).singleElement()
                .satisfies(order -> assertThat(order.result()).isNull());
        assertThat(auditLogs.count()).isGreaterThanOrEqualTo(12);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9193000000" + digits, label + "@example.com",
                "Diagnostic User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
