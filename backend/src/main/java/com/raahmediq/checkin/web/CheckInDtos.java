package com.raahmediq.checkin.web;

import com.raahmediq.appointment.domain.AppointmentStatus;
import com.raahmediq.checkin.domain.CheckInChannel;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class CheckInDtos {
    private CheckInDtos() {
    }

    public record CheckInRequest(@NotNull CheckInChannel channel) {
    }

    public record CheckInResponse(UUID id, UUID appointmentId, CheckInChannel channel, String privacyToken,
                                  Integer queuePosition, AppointmentStatus appointmentStatus, Instant checkedInAt) {
    }
}
