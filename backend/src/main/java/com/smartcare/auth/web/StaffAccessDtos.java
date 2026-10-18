package com.smartcare.auth.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class StaffAccessDtos {
    private StaffAccessDtos() {}
    public record IssueInvitation(
            @NotBlank @Pattern(regexp="^\\+?[1-9][0-9]{7,14}$") String mobileNumber,
            @NotBlank String accountType, @NotNull UUID hospitalId, UUID doctorId, UUID requestId) {
        public IssueInvitation(String mobileNumber, String accountType, UUID hospitalId, UUID doctorId) {
            this(mobileNumber, accountType, hospitalId, doctorId, null);
        }
    }
    public record InvitationIssued(UUID id, String invitationCode, Instant expiresAt) {}
    public record InvitationSummary(UUID id, String mobileNumber, String accountType,
            UUID hospitalId, UUID doctorId, Instant createdAt, Instant expiresAt, String status) {}
    public record InvitableDoctor(UUID id, String name, String registrationNumber) {}
    public record RequestStaffAccess(@NotBlank @Size(max=120) String name,
            @NotBlank @Pattern(regexp="^\\+?[1-9][0-9]{7,14}$") String mobileNumber,
            @Email @Size(max=254) String email, @NotBlank String accountType,
            @NotNull UUID hospitalId, @NotBlank @Size(max=500) String message) {}
    public record AccessRequestSummary(UUID id, String name, String mobileNumber, String email,
            String accountType, UUID hospitalId, String message, Instant createdAt) {}
    public record StaffRegistration(@NotBlank @Size(max=100) String invitationCode,
                                    @NotBlank String accountType, @NotNull @Valid RegisterRequest patient) {}
    public record ExistingStaffActivation(@NotBlank @Size(max=100) String invitationCode,
            @NotBlank String accountType, @NotBlank @Pattern(regexp="^\\+?[1-9][0-9]{7,14}$") String mobileNumber,
            @NotBlank @Size(max=72) String password) {}
}
