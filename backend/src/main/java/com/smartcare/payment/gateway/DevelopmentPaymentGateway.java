package com.smartcare.payment.gateway;

import com.smartcare.payment.domain.PaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "smartcare.payment", name = "provider", havingValue = "DEVELOPMENT",
        matchIfMissing = true)
public class DevelopmentPaymentGateway implements PaymentGateway {

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.DEVELOPMENT;
    }

    @Override
    public IntentReference createIntent(UUID appointmentId, Instant expiresAt) {
        return new IntentReference("DEV-INTENT-" + UUID.randomUUID(), null);
    }

    @Override
    public RefundReference requestRefund(UUID paymentId, String providerTransactionId) {
        return new RefundReference("DEV-REFUND-REQUEST-" + UUID.randomUUID());
    }
}
