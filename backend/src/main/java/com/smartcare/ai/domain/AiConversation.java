package com.smartcare.ai.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ai_conversations")
public class AiConversation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    protected AiConversation() {
    }

    public AiConversation(Patient patient, String title, Instant lastActivityAt) {
        this.patient = patient;
        this.title = title;
        this.lastActivityAt = lastActivityAt;
    }

    public void touch(Instant at) { this.lastActivityAt = at; }

    public void titleFromQuestion(String question) {
        if (!"New care conversation".equals(title)) return;
        String normalized = question == null ? "" : question.replaceAll("\\s+", " ").trim();
        title = normalized.length() <= 80 ? normalized : normalized.substring(0, 77) + "...";
    }

    public Patient getPatient() { return patient; }
    public String getTitle() { return title; }
    public Instant getLastActivityAt() { return lastActivityAt; }
}
