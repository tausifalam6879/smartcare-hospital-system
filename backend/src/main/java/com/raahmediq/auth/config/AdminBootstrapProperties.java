package com.raahmediq.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "raahmediq.bootstrap.admin")
public record AdminBootstrapProperties(
        boolean enabled,
        String mobileNumber,
        String email,
        String password,
        String displayName
) {
}
