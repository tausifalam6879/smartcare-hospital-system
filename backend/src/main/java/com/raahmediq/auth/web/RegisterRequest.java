package com.raahmediq.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$", message = "must contain 8 to 15 digits and may start with +")
        String mobileNumber,
        @Email String email,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(min = 10, max = 72) String password,
        @Past LocalDate dateOfBirth,
        @Size(max = 30) String gender,
        @Pattern(regexp = "^$|^\\+?[1-9][0-9]{7,14}$", message = "must contain a mobile number, not a person's name")
        String emergencyContact,
        @Size(max = 500) String address,
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "must be a language tag such as en or hi-IN")
        String preferredLanguage
) {
}
