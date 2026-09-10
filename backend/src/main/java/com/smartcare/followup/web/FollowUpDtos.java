package com.smartcare.followup.web;

import com.smartcare.followup.domain.FollowUpStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FollowUpDtos {
    private FollowUpDtos() {}

    public record StatusUpdateRequest(@NotNull FollowUpStatus status) {}

    public record FollowUpResponse(UUID id, UUID clinicalVisitId, LocalDate visitDate, LocalDate followUpDate,
                                   String hospitalName, String doctorName, String specialization,
                                   String instructions, boolean medicationReminderEnabled,
                                   FollowUpStatus status, boolean overdue, Instant patientResponseAt) {}
}
