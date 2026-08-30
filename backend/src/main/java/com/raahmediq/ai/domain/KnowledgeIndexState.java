package com.raahmediq.ai.domain;

import com.raahmediq.common.domain.AuditableEntity;
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
import java.util.UUID;

@Entity
@Table(name = "ai_knowledge_index_states")
public class KnowledgeIndexState extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private KnowledgeSourceType sourceType;

    @Column(name = "source_key", nullable = false)
    private UUID sourceKey;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeIndexStatus status;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    protected KnowledgeIndexState() {
    }

    public KnowledgeIndexState(Patient patient, KnowledgeSourceType sourceType, UUID sourceKey) {
        this.patient = patient;
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
        this.sourceHash = "pending";
        this.status = KnowledgeIndexStatus.FAILED;
    }

    public void complete(String sourceHash, KnowledgeIndexStatus status, String errorCode, Instant indexedAt) {
        this.sourceHash = sourceHash;
        this.status = status;
        this.errorCode = errorCode;
        this.indexedAt = indexedAt;
    }

    public String getSourceHash() { return sourceHash; }
    public KnowledgeIndexStatus getStatus() { return status; }
}
