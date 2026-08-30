package com.raahmediq.diagnostic.domain;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.doctor.domain.Doctor;
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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "diagnostic_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_diagnostic_order_appointment_procedure",
                columnNames = {"appointment_id", "procedure_id"}),
        @UniqueConstraint(name = "uk_diagnostic_order_queue",
                columnNames = {"procedure_id", "scheduled_date", "queue_position"})
})
public class DiagnosticOrder extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordered_by_doctor_id", nullable = false)
    private Doctor orderedByDoctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedure_id", nullable = false)
    private DiagnosticProcedure procedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiagnosticOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiagnosticPriority priority;

    @Column(name = "clinical_note", length = 2000)
    private String clinicalNote;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sample_collected_at")
    private Instant sampleCollectedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "result_verified_at")
    private Instant resultVerifiedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected DiagnosticOrder() {
    }

    public DiagnosticOrder(Appointment appointment, Doctor orderedByDoctor, DiagnosticProcedure procedure,
                           DiagnosticPriority priority, String clinicalNote) {
        this.patient = appointment.getPatient();
        this.appointment = appointment;
        this.orderedByDoctor = orderedByDoctor;
        this.procedure = procedure;
        this.priority = priority;
        this.clinicalNote = clinicalNote;
        this.status = DiagnosticOrderStatus.ORDERED;
    }

    public void schedule(LocalDate date, int position, Instant at) {
        if (status != DiagnosticOrderStatus.ORDERED) {
            throw new IllegalStateException("Only an unscheduled diagnostic order can be scheduled.");
        }
        scheduledDate = date;
        queuePosition = position;
        scheduledAt = at;
        status = DiagnosticOrderStatus.SCHEDULED;
    }

    public void collectSample(Instant at) {
        if (status != DiagnosticOrderStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled diagnostic order can be collected or checked in.");
        }
        sampleCollectedAt = at;
        status = DiagnosticOrderStatus.SAMPLE_COLLECTED;
    }

    public void startProcessing(Instant at) {
        if (status != DiagnosticOrderStatus.SCHEDULED && status != DiagnosticOrderStatus.SAMPLE_COLLECTED) {
            throw new IllegalStateException("Only a scheduled or collected diagnostic order can start processing.");
        }
        processingStartedAt = at;
        status = DiagnosticOrderStatus.IN_PROGRESS;
    }

    public void verifyResult(Instant at) {
        if (status != DiagnosticOrderStatus.SAMPLE_COLLECTED && status != DiagnosticOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("The diagnostic service must be collected or in progress before verification.");
        }
        resultVerifiedAt = at;
        status = DiagnosticOrderStatus.RESULT_VERIFIED;
    }

    public void cancel(Instant at, String reason) {
        if (status != DiagnosticOrderStatus.ORDERED && status != DiagnosticOrderStatus.SCHEDULED) {
            throw new IllegalStateException("This diagnostic order can no longer be cancelled.");
        }
        cancelledAt = at;
        cancellationReason = reason;
        status = DiagnosticOrderStatus.CANCELLED;
    }

    public Patient getPatient() { return patient; }
    public Appointment getAppointment() { return appointment; }
    public Doctor getOrderedByDoctor() { return orderedByDoctor; }
    public DiagnosticProcedure getProcedure() { return procedure; }
    public DiagnosticOrderStatus getStatus() { return status; }
    public DiagnosticPriority getPriority() { return priority; }
    public String getClinicalNote() { return clinicalNote; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public Integer getQueuePosition() { return queuePosition; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getSampleCollectedAt() { return sampleCollectedAt; }
    public Instant getProcessingStartedAt() { return processingStartedAt; }
    public Instant getResultVerifiedAt() { return resultVerifiedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
}
