package com.smartcare.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.appointment.web.AppointmentDtos.AppointmentResponse;
import com.smartcare.audit.service.AuditService;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import com.smartcare.payment.config.PaymentProperties;
import com.smartcare.payment.domain.Payment;
import com.smartcare.payment.domain.PaymentProvider;
import com.smartcare.payment.domain.PaymentStatus;
import com.smartcare.payment.domain.PaymentWebhookEvent;
import com.smartcare.payment.domain.Refund;
import com.smartcare.payment.gateway.PaymentGateway;
import com.smartcare.payment.repository.PaymentRepository;
import com.smartcare.payment.repository.PaymentWebhookEventRepository;
import com.smartcare.payment.repository.RefundRepository;
import com.smartcare.payment.web.PaymentDtos.CashConfirmationResponse;
import com.smartcare.payment.web.PaymentDtos.PaymentResponse;
import com.smartcare.payment.web.PaymentDtos.WebhookEventType;
import com.smartcare.payment.web.PaymentDtos.WebhookRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentWebhookEventRepository webhookEvents;
    private final PatientRepository patients;
    private final AppointmentService appointments;
    private final PaymentGateway gateway;
    private final PaymentProperties properties;
    private final WebhookSignatureVerifier signatures;
    private final ObjectMapper objectMapper;
    private final AuditService audit;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, RefundRepository refunds,
                          PaymentWebhookEventRepository webhookEvents, PatientRepository patients,
                          AppointmentService appointments, PaymentGateway gateway, PaymentProperties properties,
                          WebhookSignatureVerifier signatures, ObjectMapper objectMapper, AuditService audit,
                          Clock clock) {
        this.payments = payments;
        this.refunds = refunds;
        this.webhookEvents = webhookEvents;
        this.patients = patients;
        this.appointments = appointments;
        this.gateway = gateway;
        this.properties = properties;
        this.signatures = signatures;
        this.objectMapper = objectMapper;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse createIntent(UUID userId, UUID appointmentId, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        Appointment appointment = appointments.ownedOnlineReservation(userId, appointmentId);
        Payment existing = payments.findByPatientIdAndIdempotencyKey(appointment.getPatient().getId(), key)
                .orElse(null);
        if (existing != null) return toResponse(existing);
        existing = payments.findByAppointmentId(appointmentId).orElse(null);
        if (existing != null) return toResponse(existing);

        Instant expiresAt = min(appointment.getReservationExpiresAt(), clock.instant().plus(properties.intentTtl()));
        PaymentGateway.IntentReference intent = gateway.createIntent(appointmentId, expiresAt);
        Payment payment = payments.save(Payment.onlineIntent(appointment, gateway.provider(), key,
                intent.reference(), expiresAt));
        audit.record("PAYMENT_INTENT_CREATED", "PAYMENT", payment.getId(), appointment.getHospital().getId());
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        return payments.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentResponse processWebhook(String providerValue, String providerEventId, String signature,
                                          String rawBody) {
        signatures.verify(rawBody, signature);
        PaymentProvider provider;
        try {
            provider = PaymentProvider.valueOf(providerValue.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported payment provider.");
        }
        if (provider != gateway.provider()) throw new IllegalArgumentException("Payment provider is not configured.");
        if (providerEventId == null || providerEventId.isBlank() || providerEventId.length() > 120) {
            throw new IllegalArgumentException("A valid provider event ID is required.");
        }
        PaymentWebhookEvent previous = webhookEvents.findByProviderAndProviderEventId(provider, providerEventId.trim())
                .orElse(null);
        if (previous != null) return toResponse(previous.getPayment());

        WebhookRequest webhook = parseWebhook(rawBody);
        if (webhook.paymentId() == null || webhook.type() == null || webhook.occurredAt() == null) {
            throw new IllegalArgumentException("Payment webhook fields are incomplete.");
        }
        Payment payment = payments.findByIdForUpdate(webhook.paymentId())
                .orElseThrow(() -> new NotFoundException("Payment was not found."));
        if (payment.getProvider() != provider) throw new ConflictException("Payment provider does not match.");

        String outcome = switch (webhook.type()) {
            case SUCCEEDED -> processSuccess(payment, webhook);
            case FAILED -> processFailure(payment, webhook);
            case REFUNDED -> processRefunded(payment, webhook);
        };
        webhookEvents.save(new PaymentWebhookEvent(provider, providerEventId.trim(), payment, webhook.type().name(),
                signatures.payloadHash(rawBody), outcome, webhook.occurredAt()));
        audit.recordAs("PAYMENT_WEBHOOK", "PAYMENT_WEBHOOK_" + outcome, "PAYMENT", payment.getId(),
                payment.getAppointment().getHospital().getId());
        return toResponse(payment);
    }

    @Transactional
    public AppointmentResponse cancelAndRefund(UUID userId, UUID appointmentId, String reason) {
        Payment payment = payments.findByAppointmentIdForUpdate(appointmentId).orElse(null);
        AppointmentResponse response = appointments.cancel(userId, appointmentId, reason);
        if (payment == null) return response;
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            requestRefund(payment, "Appointment cancelled by patient");
        } else if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.FAILED) {
            payment.cancel();
            audit.record("PAYMENT_INTENT_CANCELLED", "PAYMENT", payment.getId(),
                    payment.getAppointment().getHospital().getId());
        }
        return response;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CASHIER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public CashConfirmationResponse confirmCash(UUID appointmentId) {
        if (payments.findByAppointmentIdForUpdate(appointmentId).isPresent()) {
            throw new ConflictException("A payment already exists for this appointment.");
        }
        Appointment appointment = appointments.appointmentForPayment(appointmentId);
        AppointmentResponse appointmentResponse = appointments.confirmCash(appointmentId);
        String receipt = "RVQ-CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Payment payment = payments.save(Payment.confirmedCash(appointment, "cash:" + appointmentId, receipt,
                clock.instant()));
        audit.record("CASH_PAYMENT_CONFIRMED", "PAYMENT", payment.getId(), appointment.getHospital().getId());
        return new CashConfirmationResponse(appointmentResponse, toResponse(payment));
    }

    @Scheduled(fixedDelayString = "${smartcare.payment.expiry-scan-ms:60000}",
            initialDelayString = "${smartcare.payment.expiry-initial-delay-ms:60000}")
    @Transactional
    public void expirePaymentIntents() {
        for (Payment payment : payments.findAllByStatusAndExpiresAtBefore(PaymentStatus.PENDING, clock.instant())) {
            payment.cancel();
            audit.recordAs("SYSTEM", "PAYMENT_INTENT_EXPIRED", "PAYMENT", payment.getId(),
                    payment.getAppointment().getHospital().getId());
        }
    }

    private String processSuccess(Payment payment, WebhookRequest webhook) {
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUND_PENDING
                || payment.getStatus() == PaymentStatus.REFUNDED) return "IGNORED_ALREADY_PROCESSED";
        if (webhook.providerTransactionId() == null || webhook.providerTransactionId().isBlank()) {
            throw new IllegalArgumentException("Successful payment event requires a provider transaction ID.");
        }
        payment.succeed(webhook.providerTransactionId().trim(), webhook.occurredAt());
        boolean confirmed = appointments.confirmOnlinePayment(payment.getAppointment().getId(), webhook.occurredAt());
        if (!confirmed) {
            requestRefund(payment, "Payment verified after the appointment reservation was no longer valid");
            return "LATE_SUCCESS_REFUND_REQUESTED";
        }
        return "SUCCEEDED";
    }

    private String processFailure(Payment payment, WebhookRequest webhook) {
        if (payment.getStatus() != PaymentStatus.PENDING) return "IGNORED_ALREADY_PROCESSED";
        payment.fail(webhook.occurredAt());
        return "FAILED";
    }

    private String processRefunded(Payment payment, WebhookRequest webhook) {
        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) return "IGNORED_ALREADY_PROCESSED";
        if (webhook.providerRefundId() == null || webhook.providerRefundId().isBlank()) {
            throw new IllegalArgumentException("Refund event requires a provider refund ID.");
        }
        Refund refund = refunds.findByPaymentId(payment.getId())
                .orElseThrow(() -> new ConflictException("Refund request was not found."));
        refund.complete(webhook.providerRefundId().trim(), webhook.occurredAt());
        payment.refunded(webhook.occurredAt());
        return "REFUNDED";
    }

    private void requestRefund(Payment payment, String reason) {
        if (refunds.findByPaymentId(payment.getId()).isPresent()) return;
        PaymentGateway.RefundReference providerRequest = gateway.requestRefund(payment.getId(),
                payment.getProviderTransactionId());
        refunds.save(new Refund(payment, reason, providerRequest.reference(), clock.instant()));
        payment.refundPending();
        audit.recordAs("SYSTEM", "REFUND_REQUESTED", "PAYMENT", payment.getId(),
                payment.getAppointment().getHospital().getId());
    }

    private PaymentResponse toResponse(Payment payment) {
        var refund = payment.getId() == null ? null : refunds.findByPaymentId(payment.getId()).orElse(null);
        String message = payment.getProvider() == PaymentProvider.DEVELOPMENT
                ? "Development provider: no money is collected; a valid signed webhook is required for confirmation."
                : "Complete payment on the provider checkout and wait for verification.";
        return new PaymentResponse(payment.getId(), payment.getAppointment().getId(), payment.getStatus(),
                payment.getProvider(), payment.getPaymentMethod().name(), payment.getAmount(), payment.getCurrency(),
                payment.getProviderReference(), payment.getReceiptNumber(), payment.getExpiresAt(),
                payment.getCompletedAt(), refund == null ? null : refund.getStatus(), null, message);
    }

    private WebhookRequest parseWebhook(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, WebhookRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Payment webhook JSON is invalid.");
        }
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required for this operation."));
    }

    private static String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new IllegalArgumentException("A valid Idempotency-Key header is required.");
        }
        return value.trim();
    }

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }
}
