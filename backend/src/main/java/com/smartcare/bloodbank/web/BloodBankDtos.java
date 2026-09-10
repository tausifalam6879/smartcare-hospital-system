package com.smartcare.bloodbank.web;

import com.smartcare.bloodbank.domain.AvailabilityStatus;
import com.smartcare.bloodbank.domain.BloodBankSourceType;
import com.smartcare.bloodbank.domain.BloodComponent;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodbank.domain.BloodRequestStatus;
import com.smartcare.bloodbank.domain.BloodUrgency;
import com.smartcare.bloodbank.domain.DonorEligibilityStatus;
import com.smartcare.bloodbank.domain.InventoryVerificationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BloodBankDtos {
    private BloodBankDtos() {
    }

    public record BloodBankRequest(
            @NotNull UUID hospitalId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 300) String addressLine,
            @NotBlank @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$") String contactNumber,
            @NotNull @DecimalMin("0.00") BigDecimal distanceKm,
            @Min(0) @Max(1440) int estimatedTransferMinutes,
            @NotNull BloodBankSourceType sourceType
    ) {
    }

    public record BloodBankResponse(UUID id, UUID hospitalId, String hospitalName, String code, String name,
                                    String addressLine, String contactNumber, BigDecimal distanceKm,
                                    int estimatedTransferMinutes, BloodBankSourceType sourceType) {
    }

    public record InventoryRequest(
            @NotNull UUID bloodBankId,
            @NotNull BloodGroup bloodGroup,
            @NotNull BloodComponent component,
            @NotBlank @Size(max = 80) String batchReference,
            @NotNull @Future LocalDate expiresOn,
            @Min(0) @Max(10000) int totalUnits,
            @NotNull InventoryVerificationStatus verificationStatus
    ) {
    }

    public record InventoryResponse(UUID id, UUID bloodBankId, String bloodBankName, BloodGroup bloodGroup,
                                    BloodComponent component, String batchReference, LocalDate expiresOn,
                                    int totalUnits, int reservedUnits, int availableUnits,
                                    InventoryVerificationStatus verificationStatus, Instant lastVerifiedAt,
                                    String verifiedBy) {
    }

    public record AvailabilityResponse(UUID bloodBankId, String bloodBankName, String addressLine,
                                       String contactNumber, BloodBankSourceType sourceType,
                                       BigDecimal distanceKm, int estimatedTransferMinutes,
                                       BloodGroup bloodGroup, BloodComponent component,
                                       AvailabilityStatus status, int availableUnits,
                                       Instant lastVerifiedAt, Instant verificationValidUntil) {
    }

    public record CreateBloodRequest(
            @NotBlank @Size(max = 24) String patientNumber,
            @NotNull UUID hospitalId,
            UUID appointmentId,
            @NotNull BloodGroup bloodGroup,
            @NotNull BloodComponent component,
            @Min(1) @Max(20) int units,
            @NotNull BloodUrgency urgency,
            @NotBlank @Size(max = 2000) String clinicalReason,
            @NotBlank @Size(max = 100) String idempotencyKey
    ) {
    }

    public record CancelBloodRequest(@NotBlank @Size(max = 300) String reason) {
    }

    public record AllocationResponse(UUID id, UUID bloodBankId, String bloodBankName, String contactNumber,
                                     BigDecimal distanceKm, int estimatedTransferMinutes, int units,
                                     LocalDate expiresOn, String status) {
    }

    public record BloodRequestResponse(UUID id, String patientNumber, UUID hospitalId, String hospitalName,
                                       UUID appointmentId, String createdBy, BloodGroup bloodGroup,
                                       BloodComponent component, int requestedUnits, int matchedUnits,
                                       BloodUrgency urgency, BloodRequestStatus status, String clinicalReason,
                                       Instant createdAt, Instant resolvedAt, String cancellationReason,
                                       List<AllocationResponse> allocations) {
    }

    public record DonorConsentRequest(
            @NotBlank @Pattern(regexp = "^(MOBILE|SMS|WHATSAPP|EMAIL)$") String contactPreference
    ) {
    }

    public record DonorVerificationRequest(@NotNull BloodGroup verifiedBloodGroup,
                                           @NotNull DonorEligibilityStatus eligibilityStatus) {
    }

    public record DonorResponse(UUID id, DonorEligibilityStatus eligibilityStatus,
                                BloodGroup verifiedBloodGroup, String contactPreference,
                                Instant consentedAt, Instant eligibilityVerifiedAt,
                                boolean consentActive) {
    }

    public record DonorMatchResponse(UUID donorOptInId, String displayName, String mobileNumber,
                                     String contactPreference, BloodGroup verifiedBloodGroup,
                                     Instant eligibilityVerifiedAt) {
    }
}
