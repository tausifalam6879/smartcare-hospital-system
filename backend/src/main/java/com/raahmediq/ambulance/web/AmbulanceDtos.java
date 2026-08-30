package com.raahmediq.ambulance.web;

import com.raahmediq.ambulance.domain.AmbulancePriority;
import com.raahmediq.ambulance.domain.AmbulanceRequestStatus;
import com.raahmediq.ambulance.domain.AmbulanceStatus;
import com.raahmediq.ambulance.domain.TransportType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AmbulanceDtos {
    private AmbulanceDtos() {
    }

    public record CreateAmbulance(
            @NotNull UUID hospitalId,
            @NotBlank @Size(max = 30) String registrationNumber,
            @NotBlank @Size(max = 40) String callSign,
            @NotBlank @Size(max = 120) String crewLabel,
            @Pattern(regexp = "^$|^\\+?[1-9][0-9]{7,14}$") String crewContact,
            @Size(max = 180) String currentArea
    ) {
    }

    public record AmbulanceResponse(UUID id, UUID hospitalId, String hospitalName, String registrationNumber,
                                    String callSign, AmbulanceStatus status, String crewLabel, String crewContact,
                                    String currentArea, BigDecimal latitude, BigDecimal longitude,
                                    Instant locationUpdatedAt, boolean active, boolean synthetic) {
    }

    public record AmbulanceAvailabilityResponse(UUID hospitalId, String hospitalName, long availableVehicles,
                                                long activeVehicles, boolean syntheticData, Instant checkedAt) {
    }

    public record CreateAmbulanceRequest(
            @NotNull UUID hospitalId,
            @Size(max = 24) String patientNumber,
            @NotNull TransportType transportType,
            @NotNull AmbulancePriority priority,
            @NotBlank @Size(max = 500) String pickupAddress,
            @Size(max = 180) String pickupLandmark,
            @NotBlank @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$") String contactNumber,
            @Size(max = 500) String assistanceNotes,
            @NotBlank @Size(max = 100) String idempotencyKey
    ) {
    }

    public record AssignAmbulance(@NotNull UUID ambulanceId) {
    }

    public record UpdateAmbulanceRequestStatus(@NotNull AmbulanceRequestStatus status,
                                               @Size(max = 300) String note) {
    }

    public record CancelAmbulanceRequest(@NotBlank @Size(max = 300) String reason) {
    }

    public record UpdateAmbulanceLocation(
            @NotBlank @Size(max = 180) String currentArea,
            @NotNull @DecimalMin("-90.000000") @DecimalMax("90.000000") BigDecimal latitude,
            @NotNull @DecimalMin("-180.000000") @DecimalMax("180.000000") BigDecimal longitude
    ) {
    }

    public record UpdateAmbulanceAvailability(@NotNull AmbulanceStatus status) {
    }

    public record AssignedAmbulanceResponse(UUID id, String registrationNumber, String callSign,
                                            AmbulanceStatus status, String currentArea,
                                            Instant locationUpdatedAt, boolean synthetic) {
    }

    public record AmbulanceEventResponse(AmbulanceRequestStatus fromStatus, AmbulanceRequestStatus toStatus,
                                         String actorLabel, String note, Instant eventAt) {
    }

    public record AmbulanceRequestResponse(UUID id, String patientNumber, UUID hospitalId, String hospitalName,
                                           String hospitalAddress, String hospitalContactNumber,
                                           TransportType transportType, AmbulancePriority priority,
                                           AmbulanceRequestStatus status, String pickupAddress,
                                           String pickupLandmark, String contactNumber, String assistanceNotes,
                                           AssignedAmbulanceResponse ambulance, Instant statusUpdatedAt,
                                           Instant dispatchedAt, Instant acknowledgedAt, Instant completedAt,
                                           Instant cancelledAt, String cancellationReason, Instant createdAt,
                                           List<AmbulanceEventResponse> timeline) {
    }
}
