package com.raahmediq.notification.domain;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.patient.domain.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 180)
    private String deduplicationKey;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(Patient patient, Appointment appointment, NotificationType type, String title,
                        String message, String deduplicationKey) {
        this.patient = patient;
        this.appointment = appointment;
        this.type = type;
        this.title = title;
        this.message = message;
        this.deduplicationKey = deduplicationKey;
    }

    public void markRead(Instant at) {
        if (readAt == null) readAt = at;
    }

    public Patient getPatient() { return patient; }
    public Appointment getAppointment() { return appointment; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public Instant getReadAt() { return readAt; }
}
