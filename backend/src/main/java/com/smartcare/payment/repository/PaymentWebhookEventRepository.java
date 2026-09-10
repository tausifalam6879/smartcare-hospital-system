package com.smartcare.payment.repository;

import com.smartcare.payment.domain.PaymentProvider;
import com.smartcare.payment.domain.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {
    Optional<PaymentWebhookEvent> findByProviderAndProviderEventId(PaymentProvider provider, String providerEventId);
}
