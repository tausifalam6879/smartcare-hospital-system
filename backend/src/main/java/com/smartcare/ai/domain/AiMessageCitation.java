package com.smartcare.ai.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_message_citations")
public class AiMessageCitation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private AiMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunk chunk;

    @Column(name = "citation_order", nullable = false)
    private int citationOrder;

    protected AiMessageCitation() {
    }

    public AiMessageCitation(AiMessage message, DocumentChunk chunk, int citationOrder) {
        this.message = message;
        this.chunk = chunk;
        this.citationOrder = citationOrder;
    }

    public DocumentChunk getChunk() { return chunk; }
    public int getCitationOrder() { return citationOrder; }
}
