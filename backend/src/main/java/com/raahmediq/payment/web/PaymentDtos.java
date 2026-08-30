package com.raahmediq.payment.web;

import com.raahmediq.appointment.web.AppointmentDtos.AppointmentResponse;
import com.raahmediq.payment.domain.PaymentProvider;
import com.raahmediq.payment.domain.PaymentStatus;
import com.raahmediq.payment.domain.RefundStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record IntentRequest(@NotNull UUID appointmentId) {
    }

    public enum WebhookEventType {
        SUCCEEDED,
        FAILED,
        REFUNDED
    }

    public record WebhookRequest(
            UUID paymentId,
            WebhookEventType type,
            String providerTransactionId,
            String providerRefundId,
            Instant occurredAt
    ) {
    }

    public record PaymentResponse(
            UUID id,
            UUID appointmentId,
            PaymentStatus status,
            PaymentProvider provider,
            String paymentMethod,
            BigDecimal amount,
            String currency,
            String providerReference,
            String receiptNumber,
            Instant expiresAt,
            Instant completedAt,
            RefundStatus refundStatus,
            String checkoutUrl,
            String verificationMessage
    ) {
    }

    public record CashConfirmationResponse(AppointmentResponse appointment, PaymentResponse payment) {
    }
}
