package com.smartcare.checkin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartcare.check-in")
public record CheckInProperties(Duration opensBefore, Duration closesAfter) {
    public CheckInProperties {
        if (opensBefore == null) opensBefore = Duration.ofHours(4);
        if (closesAfter == null) closesAfter = Duration.ofHours(2);
        if (opensBefore.isNegative() || closesAfter.isNegative()) {
            throw new IllegalArgumentException("Check-in windows cannot be negative.");
        }
    }
}
