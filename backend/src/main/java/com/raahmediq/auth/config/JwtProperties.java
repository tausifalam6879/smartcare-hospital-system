package com.raahmediq.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "raahmediq.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl) {
}
