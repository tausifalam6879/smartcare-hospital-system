package com.smartcare.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartcare.queue")
public record QueueProperties(Duration onlineReservationTtl, Duration cashGracePeriod,
                              Duration confirmedCancellationCutoff) {
    public QueueProperties {
        if (onlineReservationTtl == null) onlineReservationTtl = Duration.ofMinutes(10);
        if (cashGracePeriod == null) cashGracePeriod = Duration.ofMinutes(15);
        if (confirmedCancellationCutoff == null) confirmedCancellationCutoff = Duration.ofHours(2);
    }
}
