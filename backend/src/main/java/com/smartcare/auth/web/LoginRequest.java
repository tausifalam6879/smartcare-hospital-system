package com.smartcare.auth.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String credential, @NotBlank String password) {
}
