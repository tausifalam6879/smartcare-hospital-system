package com.smartcare.queue.web;

import com.smartcare.appointment.domain.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class QueueDtos {
    private QueueDtos() {
    }

    public record PublicQueueResponse(UUID doctorId, String doctorName, UUID hospitalId, String hospitalName,
                                      LocalDate serviceDate, Integer currentlyServingPosition,
                                      String currentlyServingToken, int checkedInWaiting,
                                      int refreshAfterSeconds, Instant lastUpdatedAt) {
    }

    public record PatientQueueResponse(UUID appointmentId, UUID doctorId, String doctorName, String hospitalName,
                                       LocalDate serviceDate, AppointmentStatus status, Integer yourPosition,
                                       String privacyToken, Integer currentlyServingPosition, int patientsAhead,
                                       int estimatedWaitMinutes, String estimateNotice, String building,
                                       String floorLabel, String roomNumber, Instant lastUpdatedAt) {
    }
}
