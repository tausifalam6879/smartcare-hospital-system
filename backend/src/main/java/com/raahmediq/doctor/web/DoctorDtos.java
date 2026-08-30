package com.raahmediq.doctor.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class DoctorDtos {
    private DoctorDtos() {
    }

    public record DoctorRequest(
            @NotNull UUID hospitalId,
            @NotNull UUID departmentId,
            @NotBlank @Size(max = 140) String name,
            @NotBlank @Size(max = 140) String specialization,
            @NotBlank @Size(max = 60) String registrationNumber,
            @NotNull @DecimalMin("0.00") BigDecimal consultationFee,
            @Min(5) @Max(240) int expectedConsultationMinutes,
            @Min(1) @Max(1000) int dailyMaxCapacity,
            @Size(max = 80) String building,
            @Size(max = 40) String floorLabel,
            @Size(max = 40) String roomNumber,
            Boolean active
    ) {
    }

    public record ScheduleRequest(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(5) @Max(240) int slotDurationMinutes,
            @Positive Integer capacityOverride
    ) {
    }

    public record ScheduleResponse(
            UUID id,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            Integer capacityOverride
    ) {
    }

    public record LinkAccountRequest(@NotNull UUID userId) {
    }

    public record DoctorResponse(
            UUID id,
            UUID hospitalId,
            String hospitalName,
            UUID departmentId,
            String departmentName,
            String name,
            String specialization,
            String registrationNumber,
            BigDecimal consultationFee,
            int expectedConsultationMinutes,
            int dailyMaxCapacity,
            String building,
            String floorLabel,
            String roomNumber,
            boolean active,
            List<ScheduleResponse> schedules
    ) {
    }
}
