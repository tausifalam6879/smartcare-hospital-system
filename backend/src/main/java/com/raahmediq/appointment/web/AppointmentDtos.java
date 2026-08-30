package com.raahmediq.appointment.web;

import com.raahmediq.appointment.domain.AppointmentStatus;
import com.raahmediq.appointment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class AppointmentDtos {
    private AppointmentDtos() {
    }

    public record BookingRequest(
            @NotNull UUID doctorId,
            @NotNull LocalDate serviceDate,
            @NotNull PaymentMethod paymentMethod
    ) {
    }

    public record CancelRequest(@Size(max = 300) String reason) {
    }

    public record AvailabilityResponse(
            UUID doctorId,
            LocalDate serviceDate,
            boolean bookable,
            int effectiveCapacity,
            int reservedCount,
            int positionsAvailable,
            long waitlistCount,
            LocalTime scheduleStart,
            LocalTime scheduleEnd
    ) {
    }

    public record AppointmentResponse(
            UUID id,
            UUID doctorId,
            String doctorName,
            String specialization,
            UUID hospitalId,
            String hospitalName,
            String departmentName,
            LocalDate serviceDate,
            Integer queuePosition,
            AppointmentStatus status,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            Instant reservationExpiresAt,
            Instant cashDeadlineAt,
            int estimatedWaitMinutes,
            String building,
            String floorLabel,
            String roomNumber,
            Instant checkedInAt,
            Instant consultationStartedAt,
            Instant completedAt,
            Instant createdAt
    ) {
    }
}
