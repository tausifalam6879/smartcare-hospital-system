package com.smartcare.ai.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiDtos {
    private AiDtos() {
    }

    public record AskRequest(
            @NotBlank @Size(min = 3, max = 600) String question
    ) {
    }

    public record ConversationResponse(UUID id, String title, Instant lastActivityAt) {
    }

    public record CitationResponse(
            UUID chunkId,
            int number,
            String sourceType,
            UUID sourceId,
            String sourceLabel,
            Integer pageNumber,
            String excerpt,
            String provenance,
            String sourcePath
    ) {
    }

    public record MessageResponse(
            UUID id,
            String role,
            String content,
            String safetyClass,
            boolean grounded,
            Instant createdAt,
            List<CitationResponse> citations
    ) {
    }

    public record AskResponse(UUID conversationId, MessageResponse message) {
    }

    public record AssistantStatusResponse(
            String mode,
            boolean patientIsolation,
            boolean citationsRequired,
            boolean externalDataSharing,
            String documentSupport
    ) {
    }
}
