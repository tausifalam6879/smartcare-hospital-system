package com.raahmediq.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "raahmediq.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
