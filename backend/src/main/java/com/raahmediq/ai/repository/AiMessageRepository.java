package com.raahmediq.ai.repository;

import com.raahmediq.ai.domain.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
