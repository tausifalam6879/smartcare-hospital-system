package com.raahmediq.notification.web;

import com.raahmediq.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(UUID id, UUID appointmentId, NotificationType type, String title,
                                       String message, boolean read, Instant readAt, Instant createdAt) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
