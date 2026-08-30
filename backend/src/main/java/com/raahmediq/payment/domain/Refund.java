package com.raahmediq.payment.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "provider_reference", nullable = false, length = 120)
    private String providerReference;

    @Column(name = "provider_refund_id", length = 120)
    private String providerRefundId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Refund() {
    }

    public Refund(Payment payment, String reason, String providerReference, Instant requestedAt) {
        this.payment = payment;
        this.amount = payment.getAmount();
        this.status = RefundStatus.REQUESTED;
        this.reason = reason;
        this.providerReference = providerReference;
        this.requestedAt = requestedAt;
    }

    public void complete(String providerRefundId, Instant at) {
        status = RefundStatus.COMPLETED;
        this.providerRefundId = providerRefundId;
        completedAt = at;
    }

    public Payment getPayment() { return payment; }
    public BigDecimal getAmount() { return amount; }
    public RefundStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getProviderReference() { return providerReference; }
    public String getProviderRefundId() { return providerRefundId; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
