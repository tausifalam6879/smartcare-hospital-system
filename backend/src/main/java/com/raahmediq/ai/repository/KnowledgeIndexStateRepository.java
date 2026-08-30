package com.raahmediq.ai.repository;

import com.raahmediq.ai.domain.KnowledgeIndexState;
import com.raahmediq.ai.domain.KnowledgeSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeIndexStateRepository extends JpaRepository<KnowledgeIndexState, UUID> {
    Optional<KnowledgeIndexState> findByPatientIdAndSourceTypeAndSourceKey(
            UUID patientId, KnowledgeSourceType sourceType, UUID sourceKey);
}
