package com.raahmediq.payment.domain;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.appointment.domain.PaymentMethod;
import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_appointment", columnNames = "appointment_id"),
        @UniqueConstraint(name = "uk_payment_idempotency", columnNames = {"patient_id", "idempotency_key"}),
        @UniqueConstraint(name = "uk_payment_provider_transaction", columnNames = {"provider", "provider_transaction_id"})
})
public class Payment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "provider_reference", nullable = false, length = 120)
    private String providerReference;

    @Column(name = "provider_transaction_id", length = 120)
    private String providerTransactionId;

    @Column(name = "receipt_number", length = 40)
    private String receiptNumber;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Payment() {
    }

    private Payment(Appointment appointment, PaymentMethod paymentMethod, PaymentStatus status,
                    PaymentProvider provider, String idempotencyKey, String providerReference, Instant expiresAt) {
        this.appointment = appointment;
        this.patient = appointment.getPatient();
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.provider = provider;
        this.amount = appointment.getAmount();
        this.currency = "INR";
        this.idempotencyKey = idempotencyKey;
        this.providerReference = providerReference;
        this.expiresAt = expiresAt;
    }

    public static Payment onlineIntent(Appointment appointment, PaymentProvider provider, String idempotencyKey,
                                       String providerReference, Instant expiresAt) {
        return new Payment(appointment, PaymentMethod.ONLINE, PaymentStatus.PENDING, provider, idempotencyKey,
                providerReference, expiresAt);
    }

    public static Payment confirmedCash(Appointment appointment, String idempotencyKey, String receiptNumber,
                                        Instant completedAt) {
        Payment payment = new Payment(appointment, PaymentMethod.CASH, PaymentStatus.SUCCEEDED,
                PaymentProvider.DEVELOPMENT, idempotencyKey, "CASH-" + appointment.getId(), null);
        payment.receiptNumber = receiptNumber;
        payment.completedAt = completedAt;
        payment.providerTransactionId = "CASH-" + appointment.getId();
        return payment;
    }

    public void succeed(String transactionId, Instant at) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.FAILED
                && status != PaymentStatus.CANCELLED) return;
        status = PaymentStatus.SUCCEEDED;
        providerTransactionId = transactionId;
        completedAt = at;
        receiptNumber = receipt("ONLINE");
        expiresAt = null;
    }

    public void fail(Instant at) {
        if (status != PaymentStatus.PENDING) return;
        status = PaymentStatus.FAILED;
        completedAt = at;
        expiresAt = null;
    }

    public void cancel() {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.FAILED) {
            status = PaymentStatus.CANCELLED;
            expiresAt = null;
        }
    }

    public void refundPending() {
        if (status == PaymentStatus.SUCCEEDED) status = PaymentStatus.REFUND_PENDING;
    }

    public void refunded(Instant at) {
        if (status != PaymentStatus.REFUND_PENDING) return;
        status = PaymentStatus.REFUNDED;
        completedAt = at;
    }

    private static String receipt(String source) {
        return "RVQ-" + source + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Appointment getAppointment() { return appointment; }
    public Patient getPatient() { return patient; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public PaymentProvider getProvider() { return provider; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getProviderReference() { return providerReference; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getReceiptNumber() { return receiptNumber; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCompletedAt() { return completedAt; }
}
