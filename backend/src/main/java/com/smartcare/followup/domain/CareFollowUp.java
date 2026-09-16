package com.smartcare.followup.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.medicalrecord.domain.ClinicalVisit;
import com.smartcare.patient.domain.Patient;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "care_follow_ups")
public class CareFollowUp extends AuditableEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinical_visit_id", nullable = false, unique = true)
    private ClinicalVisit clinicalVisit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;

    @Column(length = 2000)
    private String instructions;

    @Column(name = "medication_reminder_enabled", nullable = false)
    private boolean medicationReminderEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpStatus status;

    @Column(name = "patient_response_at")
    private Instant patientResponseAt;

    protected CareFollowUp() {}

    public CareFollowUp(ClinicalVisit visit, LocalDate followUpDate, String instructions,
                        boolean medicationReminderEnabled) {
        this.clinicalVisit = visit;
        this.patient = visit.getPatient();
        this.doctor = visit.getDoctor();
        this.hospital = visit.getHospital();
        this.followUpDate = followUpDate;
        this.instructions = instructions;
        this.medicationReminderEnabled = medicationReminderEnabled;
        this.status = FollowUpStatus.SCHEDULED;
    }

    public void updateStatus(FollowUpStatus next, Instant respondedAt) {
        if ((status == FollowUpStatus.COMPLETED || status == FollowUpStatus.MISSED) && next != status) {
            throw new IllegalArgumentException("A completed or missed follow-up cannot be changed.");
        }
        this.status = next;
        this.patientResponseAt = respondedAt;
    }

    public ClinicalVisit getClinicalVisit() { return clinicalVisit; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public Hospital getHospital() { return hospital; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public String getInstructions() { return instructions; }
    public boolean isMedicationReminderEnabled() { return medicationReminderEnabled; }
    public FollowUpStatus getStatus() { return status; }
    public Instant getPatientResponseAt() { return patientResponseAt; }
}
