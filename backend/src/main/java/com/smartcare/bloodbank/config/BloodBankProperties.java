package com.smartcare.bloodbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartcare.blood-bank")
public record BloodBankProperties(Duration verificationMaxAge) {
    public BloodBankProperties {
        if (verificationMaxAge == null || verificationMaxAge.isNegative() || verificationMaxAge.isZero()) {
            verificationMaxAge = Duration.ofHours(6);
        }
    }
}
