package com.smartcare.hospital.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class HospitalDtos {
    private HospitalDtos() {
    }

    public record HospitalRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$") String code,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 240) String addressLine,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 12) String postalCode,
            @NotBlank @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$") String contactNumber,
            @NotBlank @Size(max = 60) String timeZone,
            Boolean active
    ) {
    }

    public record DepartmentRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$") String code,
            @NotBlank @Size(max = 140) String name,
            @Size(max = 500) String description
    ) {
    }

    public record DepartmentResponse(UUID id, String code, String name, String description) {
    }

    public record HospitalResponse(
            UUID id,
            String code,
            String name,
            String addressLine,
            String city,
            String state,
            String postalCode,
            String contactNumber,
            String timeZone,
            boolean active,
            List<DepartmentResponse> departments
    ) {
    }
}
