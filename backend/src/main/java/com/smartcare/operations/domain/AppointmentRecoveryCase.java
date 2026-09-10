package com.smartcare.operations.domain;

import com.smartcare.appointment.domain.Appointment;
import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "appointment_recovery_cases")
public class AppointmentRecoveryCase extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private DoctorDayOperation operation;

    @Column(name = "original_queue_position")
    private Integer originalQueuePosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_choice", length = 40)
    private RecoveryChoice patientChoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_doctor_id")
    private Doctor targetDoctor;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected AppointmentRecoveryCase() {
    }

    public AppointmentRecoveryCase(Appointment appointment, DoctorDayOperation operation) {
        this.appointment = appointment;
        this.patient = appointment.getPatient();
        this.operation = operation;
        this.originalQueuePosition = appointment.getQueuePosition();
        this.status = RecoveryStatus.AWAITING_PATIENT_CHOICE;
    }

    public void rescheduled(RecoveryChoice choice, Doctor targetDoctor, LocalDate targetDate, Instant at) {
        requirePending();
        this.patientChoice = choice;
        this.targetDoctor = targetDoctor;
        this.targetDate = targetDate;
        this.decidedAt = at;
        this.resolvedAt = at;
        this.status = RecoveryStatus.RESCHEDULED;
    }

    public void refundReview(Instant at) {
        requirePending();
        patientChoice = RecoveryChoice.REFUND_REVIEW;
        decidedAt = at;
        resolvedAt = at;
        status = RecoveryStatus.REFUND_REVIEW_REQUIRED;
    }

    private void requirePending() {
        if (status != RecoveryStatus.AWAITING_PATIENT_CHOICE) {
            throw new IllegalStateException("This recovery case has already been resolved.");
        }
    }

    public Appointment getAppointment() { return appointment; }
    public Patient getPatient() { return patient; }
    public DoctorDayOperation getOperation() { return operation; }
    public Integer getOriginalQueuePosition() { return originalQueuePosition; }
    public RecoveryStatus getStatus() { return status; }
    public RecoveryChoice getPatientChoice() { return patientChoice; }
    public Doctor getTargetDoctor() { return targetDoctor; }
    public LocalDate getTargetDate() { return targetDate; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
