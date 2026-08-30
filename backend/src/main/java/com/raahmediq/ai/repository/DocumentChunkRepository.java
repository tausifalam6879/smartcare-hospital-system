package com.raahmediq.ai.repository;

import com.raahmediq.ai.domain.DocumentChunk;
import com.raahmediq.ai.domain.KnowledgeSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findTop200ByPatientIdOrderByCreatedAtDesc(UUID patientId);
    void deleteAllByPatientIdAndSourceTypeAndSourceKey(UUID patientId, KnowledgeSourceType sourceType, UUID sourceKey);
}
