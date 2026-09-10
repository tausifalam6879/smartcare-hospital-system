package com.smartcare.medicalrecord.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "prescriptions")
public class Prescription extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinical_visit_id", nullable = false, unique = true)
    private ClinicalVisit clinicalVisit;

    @Column(name = "general_instructions", length = 1200)
    private String generalInstructions;

    @Column(name = "prescribed_at", nullable = false)
    private Instant prescribedAt;

    protected Prescription() {
    }

    public Prescription(ClinicalVisit clinicalVisit, String generalInstructions, Instant prescribedAt) {
        this.clinicalVisit = clinicalVisit;
        this.generalInstructions = generalInstructions;
        this.prescribedAt = prescribedAt;
    }

    public ClinicalVisit getClinicalVisit() { return clinicalVisit; }
    public String getGeneralInstructions() { return generalInstructions; }
    public Instant getPrescribedAt() { return prescribedAt; }
}
