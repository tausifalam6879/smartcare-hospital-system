package com.smartcare.auth.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class StaffAccessDtos {
    private StaffAccessDtos() {}
    public record IssueInvitation(
            @NotBlank @Pattern(regexp="^\\+?[1-9][0-9]{7,14}$") String mobileNumber,
            @NotBlank String accountType, @NotNull UUID hospitalId, UUID doctorId) {}
    public record InvitationIssued(UUID id, String invitationCode, Instant expiresAt) {}
    public record StaffRegistration(@NotBlank @Size(max=100) String invitationCode,
                                    @NotBlank String accountType, @NotNull @Valid RegisterRequest patient) {}
}
