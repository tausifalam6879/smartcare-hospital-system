package com.smartcare.diagnostic.web;

import com.smartcare.diagnostic.domain.DiagnosticModality;
import com.smartcare.diagnostic.domain.DiagnosticOrderStatus;
import com.smartcare.diagnostic.domain.DiagnosticPriority;
import com.smartcare.diagnostic.domain.DiagnosticResultFlag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DiagnosticDtos {
    private DiagnosticDtos() {
    }

    public record ProcedureRequest(
            @NotNull UUID hospitalId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 180) String name,
            @NotNull DiagnosticModality modality,
            @Size(max = 1200) String preparationInstructions,
            @Min(1) @Max(720) int turnaroundHours,
            @Min(1) @Max(5000) int dailyCapacity,
            @Min(5) @Max(480) int estimatedDurationMinutes,
            @NotNull @DecimalMin("0.00") BigDecimal fee,
            @Size(max = 80) String building,
            @Size(max = 40) String floorLabel,
            @Size(max = 40) String roomNumber
    ) {
    }

    public record ProcedureResponse(UUID id, UUID hospitalId, String hospitalName, String code, String name,
                                    DiagnosticModality modality, String preparationInstructions,
                                    int turnaroundHours, int dailyCapacity, int estimatedDurationMinutes,
                                    BigDecimal fee, String building, String floorLabel, String roomNumber) {
    }

    public record AvailabilityResponse(UUID procedureId, LocalDate serviceDate, int capacity,
                                       int reserved, int remaining) {
    }

    public record CreateOrderRequest(@NotNull UUID appointmentId, @NotNull UUID procedureId,
                                     @NotNull DiagnosticPriority priority,
                                     @Size(max = 2000) String clinicalNote) {
    }

    public record ScheduleOrderRequest(@NotNull @FutureOrPresent LocalDate serviceDate) {
    }

    public record CancelOrderRequest(@Size(max = 300) String reason) {
    }

    public record ResultItemRequest(@NotBlank @Size(max = 180) String name,
                                    @NotBlank @Size(max = 180) String value,
                                    @Size(max = 80) String unit,
                                    @Size(max = 180) String referenceRange,
                                    @NotNull DiagnosticResultFlag flag) {
    }

    public record VerifyResultRequest(@NotBlank @Size(max = 2000) String summary,
                                      @Size(max = 6000) String findings,
                                      @Size(max = 3000) String impression,
                                      @NotNull DiagnosticResultFlag overallFlag,
                                      @Valid @Size(max = 100) List<ResultItemRequest> items) {
    }

    public record ResultItemResponse(String name, String value, String unit,
                                     String referenceRange, DiagnosticResultFlag flag) {
    }

    public record VerifiedResultResponse(UUID id, String summary, String findings, String impression,
                                         DiagnosticResultFlag overallFlag, String verifiedBy,
                                         Instant verifiedAt, List<ResultItemResponse> items) {
    }

    public record OrderResponse(UUID id, UUID appointmentId, String patientNumber, UUID procedureId,
                                String procedureCode, String procedureName, DiagnosticModality modality,
                                UUID hospitalId, String hospitalName, String orderedByDoctor, DiagnosticOrderStatus status,
                                DiagnosticPriority priority, String clinicalNote,
                                String preparationInstructions, int turnaroundHours,
                                BigDecimal fee, LocalDate scheduledDate, Integer queuePosition,
                                String building, String floorLabel, String roomNumber,
                                Instant orderedAt, Instant scheduledAt, Instant sampleCollectedAt,
                                Instant processingStartedAt, Instant resultVerifiedAt,
                                String cancellationReason, VerifiedResultResponse result) {
    }
}
