package com.raahmediq.payment.config;

import com.raahmediq.payment.domain.PaymentProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "raahmediq.payment")
public record PaymentProperties(PaymentProvider provider, String webhookSecret, Duration intentTtl) {
    public PaymentProperties {
        if (provider == null) provider = PaymentProvider.DEVELOPMENT;
        if (webhookSecret == null || webhookSecret.length() < 32) {
            throw new IllegalStateException("RAAHMEDIQ_PAYMENT_WEBHOOK_SECRET must contain at least 32 characters.");
        }
        if (intentTtl == null) intentTtl = Duration.ofMinutes(10);
    }
}
