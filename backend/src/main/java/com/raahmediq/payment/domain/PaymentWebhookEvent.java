package com.raahmediq.payment.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "payment_webhook_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_webhook_event", columnNames = {"provider", "provider_event_id"}))
public class PaymentWebhookEvent extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(name = "provider_event_id", nullable = false, length = 120)
    private String providerEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Column(nullable = false, length = 40)
    private String outcome;

    @Column(name = "provider_occurred_at", nullable = false)
    private Instant providerOccurredAt;

    protected PaymentWebhookEvent() {
    }

    public PaymentWebhookEvent(PaymentProvider provider, String providerEventId, Payment payment, String eventType,
                               String payloadSha256, String outcome, Instant providerOccurredAt) {
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.payment = payment;
        this.eventType = eventType;
        this.payloadSha256 = payloadSha256;
        this.outcome = outcome;
        this.providerOccurredAt = providerOccurredAt;
    }

    public Payment getPayment() { return payment; }
    public String getProviderEventId() { return providerEventId; }
}
