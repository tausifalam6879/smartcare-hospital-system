package com.raahmediq.auth.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String credential, @NotBlank String password) {
}
