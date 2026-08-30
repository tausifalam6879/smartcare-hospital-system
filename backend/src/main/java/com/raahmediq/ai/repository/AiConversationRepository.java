package com.raahmediq.ai.repository;

import com.raahmediq.ai.domain.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    List<AiConversation> findAllByPatientIdOrderByLastActivityAtDesc(UUID patientId);
    Optional<AiConversation> findByIdAndPatientId(UUID id, UUID patientId);
}
