package com.smartcare.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.domain.PaymentMethod;
import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.appointment.web.AppointmentDtos.BookingRequest;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.AuthResponse;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.doctor.service.DoctorService;
import com.smartcare.doctor.web.DoctorDtos.DoctorRequest;
import com.smartcare.doctor.web.DoctorDtos.ScheduleRequest;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.payment.config.PaymentProperties;
import com.smartcare.payment.domain.PaymentStatus;
import com.smartcare.payment.domain.RefundStatus;
import com.smartcare.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentFlowIntegrationTest {

    @Autowired PaymentService payments;
    @Autowired AppointmentService appointments;
    @Autowired AuthService auth;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;
    @Autowired PaymentProperties paymentProperties;
    @Autowired ObjectMapper objectMapper;

    @Test
    void signedWebhookIsIdempotentAndCancellationRequestsRefund() throws Exception {
        Fixture fixture = fixture("ONLINE");
        var appointment = appointments.book(fixture.patient().user().id(), "online-booking",
                new BookingRequest(fixture.doctorId(), fixture.visitDate(), PaymentMethod.ONLINE));
        var intent = payments.createIntent(fixture.patient().user().id(), appointment.id(), "online-intent");
        var repeatedIntent = payments.createIntent(fixture.patient().user().id(), appointment.id(), "online-intent");
        assertThat(intent.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(repeatedIntent.id()).isEqualTo(intent.id());

        Instant occurredAt = Instant.now();
        String successBody = objectMapper.writeValueAsString(Map.of(
                "paymentId", intent.id(),
                "type", "SUCCEEDED",
                "providerTransactionId", "DEV-TXN-1001",
                "occurredAt", occurredAt.toString()));
        var succeeded = payments.processWebhook("development", "event-success-1", sign(successBody), successBody);
        var duplicate = payments.processWebhook("development", "event-success-1", sign(successBody), successBody);
        assertThat(succeeded.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(duplicate.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(appointments.mine(fixture.patient().user().id()).get(0).status())
                .isEqualTo(AppointmentStatus.CONFIRMED);

        payments.cancelAndRefund(fixture.patient().user().id(), appointment.id(), "Cannot attend");
        assertThat(payments.mine(fixture.patient().user().id()).get(0).status())
                .isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payments.mine(fixture.patient().user().id()).get(0).refundStatus())
                .isEqualTo(RefundStatus.REQUESTED);

        String refundBody = objectMapper.writeValueAsString(Map.of(
                "paymentId", intent.id(),
                "type", "REFUNDED",
                "providerRefundId", "DEV-REFUND-1001",
                "occurredAt", Instant.now().toString()));
        var refunded = payments.processWebhook("development", "event-refund-1", sign(refundBody), refundBody);
        assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.refundStatus()).isEqualTo(RefundStatus.COMPLETED);
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void cashierConfirmationCreatesSucceededPaymentAndReceipt() {
        Fixture fixture = fixture("CASH");
        var appointment = appointments.book(fixture.patient().user().id(), "cash-booking",
                new BookingRequest(fixture.doctorId(), fixture.visitDate(), PaymentMethod.CASH));

        var confirmation = payments.confirmCash(appointment.id());
        assertThat(confirmation.appointment().status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(confirmation.payment().status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(confirmation.payment().receiptNumber()).startsWith("RVQ-CASH-");
    }

    private Fixture fixture(String suffix) {
        var hospital = hospitals.create(new HospitalRequest("SC-PAY-" + suffix,
                "Payment Test Hospital " + suffix, "3 Test Road", "Delhi", "Delhi", "110001",
                "+911112345679", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("MED", "General Medicine", "Test department"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Payment " + suffix,
                "Internal Medicine", "SC-PAY-REG-" + suffix, new BigDecimal("800.00"), 15, 10,
                "Block P", "2nd Floor", "OPD 20", true));
        LocalDate visitDate = LocalDate.now().plusDays(3);
        doctors.addSchedule(doctor.id(), new ScheduleRequest(visitDate.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(13, 0), 15, 10));
        String digits = suffix.equals("ONLINE") ? "11" : "12";
        AuthResponse patient = auth.register(new RegisterRequest("+9190000000" + digits,
                "payment." + suffix.toLowerCase() + "@example.com", "Payment Patient " + suffix,
                "safe-test-password", LocalDate.of(1990, 1, 1), null, null, null, "en"));
        return new Fixture(patient, doctor.id(), visitDate);
    }

    private String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(paymentProperties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private record Fixture(AuthResponse patient, UUID doctorId, LocalDate visitDate) {
    }
}
