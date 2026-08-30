package com.raahmediq.notification.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "notification_deliveries", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_delivery_channel", columnNames = {"notification_id", "channel"}))
public class NotificationDelivery extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DeliveryStatus status;

    @Column(length = 300)
    private String detail;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(Notification notification, NotificationChannel channel, DeliveryStatus status,
                                String detail, Instant attemptedAt) {
        this.notification = notification;
        this.channel = channel;
        this.status = status;
        this.detail = detail;
        this.attemptedAt = attemptedAt;
    }

    public NotificationChannel getChannel() { return channel; }
    public DeliveryStatus getStatus() { return status; }
    public String getDetail() { return detail; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
