package com.smartcare.bloodreaction.domain;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "blood_reaction_panels")
public class BloodReactionPanel extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    private UserAccount submittedBy;

    @Column(name = "anti_a_storage_key", nullable = false, unique = true, length = 300)
    private String antiAStorageKey;
    @Column(name = "anti_b_storage_key", nullable = false, unique = true, length = 300)
    private String antiBStorageKey;
    @Column(name = "anti_d_storage_key", nullable = false, unique = true, length = 300)
    private String antiDStorageKey;
    @Column(name = "anti_a_filename", nullable = false, length = 180)
    private String antiAFilename;
    @Column(name = "anti_b_filename", nullable = false, length = 180)
    private String antiBFilename;
    @Column(name = "anti_d_filename", nullable = false, length = 180)
    private String antiDFilename;

    @Column(name = "anti_a_probability", nullable = false, precision = 7, scale = 6)
    private BigDecimal antiAProbability;
    @Column(name = "anti_b_probability", nullable = false, precision = 7, scale = 6)
    private BigDecimal antiBProbability;
    @Column(name = "anti_d_probability", nullable = false, precision = 7, scale = 6)
    private BigDecimal antiDProbability;
    @Column(name = "anti_a_confidence", nullable = false, precision = 7, scale = 6)
    private BigDecimal antiAConfidence;
    @Column(name = "anti_b_confidence", nullable = false, precision = 7, scale = 6)
    private BigDecimal antiBConfidence;
    @Column(name = "anti_d_confidence", nullable = false, precision = 7, scale = 6)
    private BigDecimal antiDConfidence;
    @Column(name = "model_name", nullable = false, length = 120)
    private String modelName;
    @Column(name = "model_version", nullable = false, length = 80)
    private String modelVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_group", length = 30)
    private BloodGroup suggestedGroup;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodReactionPanelStatus status;
    @Column(nullable = false, length = 1000)
    private String explanation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private UserAccount reviewedBy;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "review_note", length = 500)
    private String reviewNote;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    protected BloodReactionPanel() { }

    public BloodReactionPanel(Patient patient, UserAccount submittedBy, String antiAStorageKey, String antiBStorageKey,
                              String antiDStorageKey, String antiAFilename, String antiBFilename, String antiDFilename,
                              BigDecimal antiAProbability, BigDecimal antiBProbability, BigDecimal antiDProbability,
                              BigDecimal antiAConfidence, BigDecimal antiBConfidence, BigDecimal antiDConfidence,
                              String modelName, String modelVersion, BloodGroup suggestedGroup,
                              BloodReactionPanelStatus status, String explanation) {
        this.patient = patient; this.submittedBy = submittedBy;
        this.antiAStorageKey = antiAStorageKey; this.antiBStorageKey = antiBStorageKey; this.antiDStorageKey = antiDStorageKey;
        this.antiAFilename = antiAFilename; this.antiBFilename = antiBFilename; this.antiDFilename = antiDFilename;
        this.antiAProbability = antiAProbability; this.antiBProbability = antiBProbability; this.antiDProbability = antiDProbability;
        this.antiAConfidence = antiAConfidence; this.antiBConfidence = antiBConfidence; this.antiDConfidence = antiDConfidence;
        this.modelName = modelName; this.modelVersion = modelVersion; this.suggestedGroup = suggestedGroup;
        this.status = status; this.explanation = explanation;
    }

    public Patient getPatient() { return patient; }
    public String getAntiAFilename() { return antiAFilename; }
    public String getAntiBFilename() { return antiBFilename; }
    public String getAntiDFilename() { return antiDFilename; }
    public BigDecimal getAntiAProbability() { return antiAProbability; }
    public BigDecimal getAntiBProbability() { return antiBProbability; }
    public BigDecimal getAntiDProbability() { return antiDProbability; }
    public BigDecimal getAntiAConfidence() { return antiAConfidence; }
    public BigDecimal getAntiBConfidence() { return antiBConfidence; }
    public BigDecimal getAntiDConfidence() { return antiDConfidence; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    public BloodGroup getSuggestedGroup() { return suggestedGroup; }
    public BloodReactionPanelStatus getStatus() { return status; }
    public String getExplanation() { return explanation; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
    public String getRejectionReason() { return rejectionReason; }
    public void verify(UserAccount reviewer, BloodGroup confirmedGroup, String note, Instant at) {
        if (status == BloodReactionPanelStatus.CLINICIAN_VERIFIED || status == BloodReactionPanelStatus.REJECTED) throw new IllegalStateException("This panel is already closed.");
        this.suggestedGroup = confirmedGroup; this.reviewedBy = reviewer; this.reviewedAt = at; this.reviewNote = note; this.status = BloodReactionPanelStatus.CLINICIAN_VERIFIED;
    }
    public void reject(UserAccount reviewer, String reason, Instant at) {
        if (status == BloodReactionPanelStatus.CLINICIAN_VERIFIED || status == BloodReactionPanelStatus.REJECTED) throw new IllegalStateException("This panel is already closed.");
        this.reviewedBy = reviewer; this.reviewedAt = at; this.rejectionReason = reason; this.status = BloodReactionPanelStatus.REJECTED;
    }
}
