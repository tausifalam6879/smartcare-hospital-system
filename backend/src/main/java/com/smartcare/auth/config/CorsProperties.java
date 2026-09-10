package com.smartcare.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "smartcare.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
