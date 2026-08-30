package com.raahmediq.medicalrecord.domain;

import com.raahmediq.auth.domain.UserAccount;
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

import java.time.LocalDate;

@Entity
@Table(name = "medical_documents")
public class MedicalDocument extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private UserAccount uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "original_filename", nullable = false, length = 240)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, unique = true, length = 320)
    private String storageKey;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Column(length = 600)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private DocumentVerificationStatus verificationStatus;

    protected MedicalDocument() {
    }

    public MedicalDocument(Patient patient, Hospital hospital, UserAccount uploadedBy, DocumentType documentType,
                           String originalFilename, String contentType, long sizeBytes, String storageKey,
                           String sha256, LocalDate documentDate, String description) {
        this.patient = patient;
        this.hospital = hospital;
        this.uploadedBy = uploadedBy;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.documentDate = documentDate;
        this.description = description;
        this.verificationStatus = DocumentVerificationStatus.PATIENT_UPLOADED;
    }

    public Patient getPatient() { return patient; }
    public Hospital getHospital() { return hospital; }
    public DocumentType getDocumentType() { return documentType; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public String getSha256() { return sha256; }
    public LocalDate getDocumentDate() { return documentDate; }
    public String getDescription() { return description; }
    public DocumentVerificationStatus getVerificationStatus() { return verificationStatus; }
}
