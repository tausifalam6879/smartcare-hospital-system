package com.raahmediq.ai.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_messages")
public class AiMessage extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_class", nullable = false, length = 40)
    private AiSafetyClass safetyClass;

    @Column(nullable = false)
    private boolean grounded;

    protected AiMessage() {
    }

    public AiMessage(AiConversation conversation, AiMessageRole role, String content,
                     AiSafetyClass safetyClass, boolean grounded) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.safetyClass = safetyClass;
        this.grounded = grounded;
    }

    public AiConversation getConversation() { return conversation; }
    public AiMessageRole getRole() { return role; }
    public String getContent() { return content; }
    public AiSafetyClass getSafetyClass() { return safetyClass; }
    public boolean isGrounded() { return grounded; }
}
