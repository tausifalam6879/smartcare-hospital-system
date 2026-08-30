package com.raahmediq.auth.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserSummary user
) {
    public record UserSummary(
            UUID id,
            String displayName,
            String mobileNumber,
            String email,
            String patientNumber,
            List<String> roles
    ) {
    }
}
