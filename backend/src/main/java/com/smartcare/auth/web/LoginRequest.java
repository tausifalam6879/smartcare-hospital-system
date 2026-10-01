package com.smartcare.auth.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @jakarta.validation.constraints.Size(max=254) String credential,
                           @NotBlank @jakarta.validation.constraints.Size(max=72) String password,
                           @jakarta.validation.constraints.Size(max=40) String accountType) {
    public LoginRequest(String credential, String password) { this(credential, password, null); }
}
