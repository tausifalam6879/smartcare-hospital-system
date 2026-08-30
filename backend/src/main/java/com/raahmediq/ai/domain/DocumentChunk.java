package com.raahmediq.ai.domain;

import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.hospital.domain.Hospital;
import com.raahmediq.medicalrecord.domain.ClinicalVisit;
import com.raahmediq.medicalrecord.domain.MedicalDocument;
import com.raahmediq.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "document_chunk_metadata")
public class DocumentChunk extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private KnowledgeSourceType sourceType;

    @Column(name = "source_key", nullable = false)
    private UUID sourceKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_document_id")
    private MedicalDocument medicalDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_visit_id")
    private ClinicalVisit clinicalVisit;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "citation_label", nullable = false, length = 300)
    private String citationLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding_model", nullable = false, length = 60)
    private String embeddingModel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "extracted_text", nullable = false)
    private boolean extractedText;

    protected DocumentChunk() {
    }

    public DocumentChunk(Patient patient, Hospital hospital, KnowledgeSourceType sourceType, UUID sourceKey,
                         MedicalDocument medicalDocument, ClinicalVisit clinicalVisit, int chunkIndex,
                         Integer pageNumber, String citationLabel, String content, String embeddingModel,
                         String embedding, String contentHash, boolean extractedText) {
        this.patient = patient;
        this.hospital = hospital;
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
        this.medicalDocument = medicalDocument;
        this.clinicalVisit = clinicalVisit;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.citationLabel = citationLabel;
        this.content = content;
        this.embeddingModel = embeddingModel;
        this.embedding = embedding;
        this.contentHash = contentHash;
        this.extractedText = extractedText;
    }

    public Patient getPatient() { return patient; }
    public KnowledgeSourceType getSourceType() { return sourceType; }
    public UUID getSourceKey() { return sourceKey; }
    public MedicalDocument getMedicalDocument() { return medicalDocument; }
    public ClinicalVisit getClinicalVisit() { return clinicalVisit; }
    public Integer getPageNumber() { return pageNumber; }
    public String getCitationLabel() { return citationLabel; }
    public String getContent() { return content; }
    public String getEmbedding() { return embedding; }
    public boolean isExtractedText() { return extractedText; }
}
