package com.raahmediq.medicalrecord.domain;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.auth.domain.UserAccount;
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

import java.time.Instant;

@Entity
@Table(name = "patient_allergies")
public class PatientAllergy extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private UserAccount recordedBy;

    @Column(nullable = false, length = 180)
    private String substance;

    @Column(length = 500)
    private String reaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllergySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllergyStatus status;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected PatientAllergy() {
    }

    public PatientAllergy(Appointment appointment, UserAccount recordedBy, String substance, String reaction,
                          AllergySeverity severity, Instant recordedAt) {
        this.patient = appointment.getPatient();
        this.doctor = appointment.getDoctor();
        this.appointment = appointment;
        this.recordedBy = recordedBy;
        this.substance = substance;
        this.reaction = reaction;
        this.severity = severity;
        this.status = AllergyStatus.ACTIVE;
        this.recordedAt = recordedAt;
    }

    public String getSubstance() { return substance; }
    public String getReaction() { return reaction; }
    public AllergySeverity getSeverity() { return severity; }
    public AllergyStatus getStatus() { return status; }
    public Doctor getDoctor() { return doctor; }
    public Instant getRecordedAt() { return recordedAt; }
}
