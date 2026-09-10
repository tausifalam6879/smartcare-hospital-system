package com.smartcare.ai.service;

import com.smartcare.ai.domain.DocumentChunk;
import com.smartcare.ai.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AiRetrievalService {
    private static final double MINIMUM_SCORE = 0.075;
    private final DocumentChunkRepository chunks;
    private final LocalHashEmbedding embeddings;

    public AiRetrievalService(DocumentChunkRepository chunks, LocalHashEmbedding embeddings) {
        this.chunks = chunks;
        this.embeddings = embeddings;
    }

    public List<RetrievedEvidence> retrieve(UUID patientId, String question) {
        // This database predicate is the security boundary. Ranking never sees another patient's chunks.
        return chunks.findTop200ByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(chunk -> new RetrievedEvidence(chunk,
                        embeddings.similarity(question, chunk.getEmbedding(), chunk.getCitationLabel() + "\n" + chunk.getContent())))
                .filter(item -> item.score() >= MINIMUM_SCORE)
                .sorted(Comparator.comparingDouble(RetrievedEvidence::score).reversed())
                .limit(4)
                .toList();
    }

    public record RetrievedEvidence(DocumentChunk chunk, double score) {
    }
}
