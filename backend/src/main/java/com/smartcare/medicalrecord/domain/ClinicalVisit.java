package com.smartcare.medicalrecord.domain;

import com.smartcare.appointment.domain.Appointment;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "clinical_visits")
public class ClinicalVisit extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private UserAccount recordedBy;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(length = 2000)
    private String symptoms;

    @Column(nullable = false, length = 2000)
    private String diagnosis;

    @Column(name = "doctor_notes", length = 4000)
    private String doctorNotes;

    @Column(name = "discharge_summary", length = 4000)
    private String dischargeSummary;

    @Column(name = "follow_up_recommendation", length = 2000)
    private String followUpRecommendation;

    @Column(name = "finalized_at", nullable = false)
    private Instant finalizedAt;

    protected ClinicalVisit() {
    }

    public ClinicalVisit(Appointment appointment, UserAccount recordedBy, String symptoms, String diagnosis,
                         String doctorNotes, String dischargeSummary, String followUpRecommendation,
                         Instant finalizedAt) {
        this.patient = appointment.getPatient();
        this.doctor = appointment.getDoctor();
        this.hospital = appointment.getHospital();
        this.appointment = appointment;
        this.recordedBy = recordedBy;
        this.visitDate = appointment.getServiceDate();
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.doctorNotes = doctorNotes;
        this.dischargeSummary = dischargeSummary;
        this.followUpRecommendation = followUpRecommendation;
        this.finalizedAt = finalizedAt;
    }

    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public Hospital getHospital() { return hospital; }
    public Appointment getAppointment() { return appointment; }
    public LocalDate getVisitDate() { return visitDate; }
    public String getSymptoms() { return symptoms; }
    public String getDiagnosis() { return diagnosis; }
    public String getDoctorNotes() { return doctorNotes; }
    public String getDischargeSummary() { return dischargeSummary; }
    public String getFollowUpRecommendation() { return followUpRecommendation; }
    public Instant getFinalizedAt() { return finalizedAt; }
}
