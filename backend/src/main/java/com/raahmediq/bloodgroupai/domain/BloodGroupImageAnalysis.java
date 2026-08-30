package com.raahmediq.bloodgroupai.domain;

import com.raahmediq.auth.domain.UserAccount;
import com.raahmediq.bloodbank.domain.BloodGroup;
import com.raahmediq.common.domain.AuditableEntity;
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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "blood_group_image_analyses")
public class BloodGroupImageAnalysis extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    private UserAccount submittedBy;

    @Column(name = "storage_key", nullable = false, unique = true, length = 300)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 180)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BloodGroupAnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_inference_status", nullable = false, length = 40)
    private ModelInferenceStatus modelInferenceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_suggested_group", length = 30)
    private BloodGroup modelSuggestedGroup;

    @Column(name = "model_confidence", precision = 5, scale = 4)
    private BigDecimal modelConfidence;

    @Column(name = "anti_a_reactive")
    private Boolean antiAReactive;

    @Column(name = "anti_b_reactive")
    private Boolean antiBReactive;

    @Column(name = "anti_d_reactive")
    private Boolean antiDReactive;

    @Enumerated(EnumType.STRING)
    @Column(name = "preliminary_group", length = 30)
    private BloodGroup preliminaryGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observed_by_user_id")
    private UserAccount observedBy;

    @Column(name = "observation_note", length = 500)
    private String observationNote;

    @Column(name = "observed_at")
    private Instant observedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_group", length = 30)
    private BloodGroup verifiedGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private UserAccount verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id")
    private UserAccount rejectedBy;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    protected BloodGroupImageAnalysis() {
    }

    public BloodGroupImageAnalysis(Patient patient, Hospital hospital, UserAccount submittedBy,
                                   String storageKey, String originalFilename, String contentType,
                                   long sizeBytes, String sha256, ModelInferenceStatus inferenceStatus,
                                   BloodGroup modelSuggestedGroup, BigDecimal modelConfidence) {
        this.patient = patient;
        this.hospital = hospital;
        this.submittedBy = submittedBy;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.status = BloodGroupAnalysisStatus.SUBMITTED;
        this.modelInferenceStatus = inferenceStatus;
        this.modelSuggestedGroup = modelSuggestedGroup;
        this.modelConfidence = modelConfidence;
    }

    public void recordObservations(boolean antiA, boolean antiB, boolean antiD, UserAccount observer,
                                   String note, Instant at) {
        if (status != BloodGroupAnalysisStatus.SUBMITTED) {
            throw new IllegalStateException("Observations can be recorded only once on a submitted analysis.");
        }
        antiAReactive = antiA;
        antiBReactive = antiB;
        antiDReactive = antiD;
        preliminaryGroup = interpret(antiA, antiB, antiD);
        observedBy = observer;
        observationNote = blankToNull(note);
        observedAt = at;
        status = BloodGroupAnalysisStatus.OBSERVATIONS_RECORDED;
    }

    public void verify(BloodGroup confirmedGroup, UserAccount verifier, Instant at) {
        if (status != BloodGroupAnalysisStatus.OBSERVATIONS_RECORDED) {
            throw new IllegalStateException("Lab verification requires recorded observations.");
        }
        if (observedBy.getId().equals(verifier.getId())) {
            throw new IllegalStateException("Independent verification must be completed by another authorized staff member.");
        }
        if (confirmedGroup != preliminaryGroup) {
            throw new IllegalArgumentException("Confirmed group does not match the recorded Anti-A/Anti-B/Anti-D reactions.");
        }
        verifiedGroup = confirmedGroup;
        verifiedBy = verifier;
        verifiedAt = at;
        status = BloodGroupAnalysisStatus.LAB_VERIFIED;
    }

    public void reject(UserAccount actor, String reason, Instant at) {
        if (status == BloodGroupAnalysisStatus.LAB_VERIFIED || status == BloodGroupAnalysisStatus.REJECTED) {
            throw new IllegalStateException("This analysis is already final.");
        }
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A rejection reason is required.");
        rejectedBy = actor;
        rejectionReason = reason.trim();
        rejectedAt = at;
        status = BloodGroupAnalysisStatus.REJECTED;
    }

    private static BloodGroup interpret(boolean antiA, boolean antiB, boolean antiD) {
        if (antiA && antiB) return antiD ? BloodGroup.AB_POSITIVE : BloodGroup.AB_NEGATIVE;
        if (antiA) return antiD ? BloodGroup.A_POSITIVE : BloodGroup.A_NEGATIVE;
        if (antiB) return antiD ? BloodGroup.B_POSITIVE : BloodGroup.B_NEGATIVE;
        return antiD ? BloodGroup.O_POSITIVE : BloodGroup.O_NEGATIVE;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Patient getPatient() { return patient; }
    public Hospital getHospital() { return hospital; }
    public UserAccount getSubmittedBy() { return submittedBy; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public BloodGroupAnalysisStatus getStatus() { return status; }
    public ModelInferenceStatus getModelInferenceStatus() { return modelInferenceStatus; }
    public BloodGroup getModelSuggestedGroup() { return modelSuggestedGroup; }
    public BigDecimal getModelConfidence() { return modelConfidence; }
    public Boolean getAntiAReactive() { return antiAReactive; }
    public Boolean getAntiBReactive() { return antiBReactive; }
    public Boolean getAntiDReactive() { return antiDReactive; }
    public BloodGroup getPreliminaryGroup() { return preliminaryGroup; }
    public UserAccount getObservedBy() { return observedBy; }
    public String getObservationNote() { return observationNote; }
    public Instant getObservedAt() { return observedAt; }
    public BloodGroup getVerifiedGroup() { return verifiedGroup; }
    public UserAccount getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public UserAccount getRejectedBy() { return rejectedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getRejectedAt() { return rejectedAt; }
}
