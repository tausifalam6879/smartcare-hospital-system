package com.raahmediq.appointment.domain;

import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.doctor.domain.Doctor;
import com.raahmediq.hospital.domain.Hospital;
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
import java.time.LocalDate;

@Entity
@Table(name = "appointments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_appointment_queue_position", columnNames = {"doctor_id", "service_date", "queue_position"}),
        @UniqueConstraint(name = "uk_appointment_idempotency", columnNames = {"patient_id", "idempotency_key"})
})
public class Appointment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "reservation_expires_at")
    private Instant reservationExpiresAt;

    @Column(name = "cash_deadline_at")
    private Instant cashDeadlineAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "consultation_started_at")
    private Instant consultationStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "no_show_at")
    private Instant noShowAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected Appointment() {
    }

    public Appointment(Patient patient, Doctor doctor, LocalDate serviceDate, PaymentMethod paymentMethod,
                       BigDecimal amount, String idempotencyKey) {
        this.patient = patient;
        this.doctor = doctor;
        this.hospital = doctor.getHospital();
        this.serviceDate = serviceDate;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = AppointmentStatus.WAITLISTED;
    }

    public void reserve(int position, Instant onlineExpiry, Instant cashDeadline) {
        this.queuePosition = position;
        if (paymentMethod == PaymentMethod.ONLINE) {
            status = AppointmentStatus.RESERVED_PENDING_PAYMENT;
            reservationExpiresAt = onlineExpiry;
            cashDeadlineAt = null;
        } else {
            status = AppointmentStatus.CASH_PENDING;
            cashDeadlineAt = cashDeadline;
            reservationExpiresAt = null;
        }
    }

    public void confirm(Instant at) {
        status = AppointmentStatus.CONFIRMED;
        confirmedAt = at;
        reservationExpiresAt = null;
        cashDeadlineAt = null;
    }

    public void cancel(Instant at, String reason) {
        status = AppointmentStatus.CANCELLED;
        cancelledAt = at;
        cancellationReason = reason;
        reservationExpiresAt = null;
        cashDeadlineAt = null;
    }

    public void expire() {
        status = AppointmentStatus.EXPIRED;
        reservationExpiresAt = null;
        cashDeadlineAt = null;
    }

    public void checkIn(Instant at) {
        if (status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed appointment can be checked in.");
        }
        status = AppointmentStatus.CHECKED_IN;
        checkedInAt = at;
    }

    public void startConsultation(Instant at) {
        if (status != AppointmentStatus.CHECKED_IN) {
            throw new IllegalStateException("Only a checked-in appointment can enter consultation.");
        }
        status = AppointmentStatus.IN_CONSULTATION;
        consultationStartedAt = at;
    }

    public void complete(Instant at) {
        if (status != AppointmentStatus.IN_CONSULTATION) {
            throw new IllegalStateException("Only an active consultation can be completed.");
        }
        status = AppointmentStatus.COMPLETED;
        completedAt = at;
    }

    public void markNoShow(Instant at) {
        if (status != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed appointment can be marked as no-show.");
        }
        status = AppointmentStatus.NO_SHOW;
        noShowAt = at;
    }

    public void reschedule(Doctor targetDoctor, LocalDate targetDate, int targetPosition, Instant at) {
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.CHECKED_IN) {
            throw new IllegalStateException("Only a confirmed or checked-in appointment can be recovered.");
        }
        doctor = targetDoctor;
        hospital = targetDoctor.getHospital();
        serviceDate = targetDate;
        queuePosition = targetPosition;
        status = AppointmentStatus.CONFIRMED;
        confirmedAt = at;
        checkedInAt = null;
        consultationStartedAt = null;
        completedAt = null;
        noShowAt = null;
        cancelledAt = null;
        cancellationReason = null;
        reservationExpiresAt = null;
        cashDeadlineAt = null;
    }

    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public Hospital getHospital() { return hospital; }
    public LocalDate getServiceDate() { return serviceDate; }
    public Integer getQueuePosition() { return queuePosition; }
    public AppointmentStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getReservationExpiresAt() { return reservationExpiresAt; }
    public Instant getCashDeadlineAt() { return cashDeadlineAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public Instant getConsultationStartedAt() { return consultationStartedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getNoShowAt() { return noShowAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
}
