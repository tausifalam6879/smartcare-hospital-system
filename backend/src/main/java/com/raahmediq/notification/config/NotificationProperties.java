package com.raahmediq.notification.config;

import com.raahmediq.notification.domain.NotificationChannel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "raahmediq.notifications")
public record NotificationProperties(List<NotificationChannel> channels) {
    public NotificationProperties {
        channels = channels == null || channels.isEmpty() ? List.of(NotificationChannel.IN_APP) : List.copyOf(channels);
    }
}
