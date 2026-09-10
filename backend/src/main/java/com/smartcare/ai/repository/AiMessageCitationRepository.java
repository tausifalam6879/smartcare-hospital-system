package com.smartcare.ai.repository;

import com.smartcare.ai.domain.AiMessageCitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageCitationRepository extends JpaRepository<AiMessageCitation, UUID> {
    List<AiMessageCitation> findAllByMessageIdOrderByCitationOrderAsc(UUID messageId);
}
