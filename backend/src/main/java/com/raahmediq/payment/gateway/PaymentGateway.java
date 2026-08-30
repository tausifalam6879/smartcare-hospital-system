package com.raahmediq.payment.gateway;

import com.raahmediq.payment.domain.PaymentProvider;

import java.time.Instant;
import java.util.UUID;

public interface PaymentGateway {
    PaymentProvider provider();
    IntentReference createIntent(UUID appointmentId, Instant expiresAt);
    RefundReference requestRefund(UUID paymentId, String providerTransactionId);

    record IntentReference(String reference, String checkoutUrl) {
    }

    record RefundReference(String reference) {
    }
}
