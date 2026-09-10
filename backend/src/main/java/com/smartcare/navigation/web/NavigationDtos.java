package com.smartcare.navigation.web;

import com.smartcare.navigation.domain.LocationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class NavigationDtos {
    private NavigationDtos() {
    }

    public record LocationRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,40}$") String code,
            @NotBlank @Size(max = 140) String nameEn,
            @NotBlank @Size(max = 180) String nameHi,
            @NotNull LocationType type,
            @NotBlank @Size(max = 80) String building,
            @NotBlank @Size(max = 40) String floorLabel,
            @Size(max = 80) String zone,
            @Size(max = 40) String roomNumber,
            @Min(0) @Max(100) int mapX,
            @Min(0) @Max(100) int mapY
    ) {
    }

    public record PathRequest(
            @NotBlank String fromCode,
            @NotBlank String toCode,
            @NotBlank @Size(max = 400) String instructionEn,
            @NotBlank @Size(max = 500) String instructionHi,
            @NotBlank @Size(max = 400) String reverseInstructionEn,
            @NotBlank @Size(max = 500) String reverseInstructionHi,
            @Min(1) int distanceMeters,
            @Min(1) int durationSeconds,
            boolean stepFree
    ) {
    }

    public record CheckpointRequest(
            @NotBlank String locationCode,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{4,64}$") String publicCode,
            @NotBlank @Size(max = 160) String labelEn,
            @NotBlank @Size(max = 200) String labelHi
    ) {
    }

    public record LocationResponse(String code, String nameEn, String nameHi, LocationType type,
                                   String building, String floorLabel, String zone, String roomNumber,
                                   int mapX, int mapY) {
    }

    public record CheckpointResponse(UUID hospitalId, String hospitalName, String publicCode,
                                     String labelEn, String labelHi, String entryPath,
                                     LocationResponse location) {
    }

    public record HospitalMapResponse(UUID hospitalId, String hospitalName, List<LocationResponse> locations,
                                      List<CheckpointResponse> checkpoints) {
    }

    public record RouteStep(int order, String instruction, int distanceMeters, int durationSeconds,
                            String fromCode, String toCode, String fromFloor, String toFloor,
                            boolean floorTransition, boolean stepFree) {
    }

    public record RouteResponse(boolean available, String language, boolean stepFreeRequested,
                                LocationResponse source, LocationResponse destination,
                                int totalDistanceMeters, int estimatedMinutes, List<String> pathCodes,
                                List<RouteStep> steps, String message, String safetyNotice) {
    }

    public record AppointmentDestinationResponse(UUID appointmentId, UUID hospitalId, String hospitalName,
                                                 String doctorName, LocationResponse destination,
                                                 boolean exactRoomMatch, String guidanceEn, String guidanceHi) {
    }
}
