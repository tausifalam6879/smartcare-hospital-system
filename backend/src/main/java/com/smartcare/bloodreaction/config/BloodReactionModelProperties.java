package com.smartcare.bloodreaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcare.blood-reaction-model")
public record BloodReactionModelProperties(String baseUrl) {
}
