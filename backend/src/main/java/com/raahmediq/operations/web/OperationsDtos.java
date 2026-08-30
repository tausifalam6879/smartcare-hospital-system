package com.raahmediq.operations.web;

import com.raahmediq.appointment.domain.AppointmentStatus;
import com.raahmediq.appointment.domain.PaymentMethod;
import com.raahmediq.operations.domain.DoctorDayStatus;
import com.raahmediq.operations.domain.RecoveryChoice;
import com.raahmediq.operations.domain.RecoveryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OperationsDtos {
    private OperationsDtos() {
    }

    public record UpdateDoctorDayStatus(@NotNull UUID doctorId, @NotNull LocalDate serviceDate,
                                        @NotNull DoctorDayStatus status, @Size(max = 300) String reason) {
    }

    public record DoctorDayStatusResponse(UUID id, UUID doctorId, String doctorName, UUID hospitalId,
                                          LocalDate serviceDate, DoctorDayStatus status, int delayMinutes,
                                          String reason, Instant updatedAt) {
    }

    public record ResolveRecovery(@NotNull RecoveryChoice choice, UUID targetDoctorId, LocalDate targetDate) {
    }

    public record RecoveryCaseResponse(UUID id, UUID appointmentId, RecoveryStatus status,
                                       RecoveryChoice patientChoice, UUID hospitalId, String hospitalName,
                                       UUID departmentId, String departmentName,
                                       UUID originalDoctorId, String originalDoctorName, LocalDate originalDate,
                                       Integer originalQueuePosition, String interruptionReason,
                                       UUID targetDoctorId, String targetDoctorName, LocalDate targetDate,
                                       Instant decidedAt, Instant createdAt) {
    }

    public record QueueRow(UUID appointmentId, String patientNumber, UUID doctorId, String doctorName,
                           Integer queuePosition, AppointmentStatus status, PaymentMethod paymentMethod) {
    }

    public record OperationsDashboardResponse(
            UUID hospitalId, String hospitalName, LocalDate serviceDate,
            int totalAppointments, int confirmed, int checkedIn, int inConsultation,
            int completed, int waitlisted, int noShows, int pendingPayments,
            int activeDoctors, int delayedOrUnavailableDoctors, int diagnosticLoad,
            int bloodInventoryAlerts, long ambulancesAvailable, long ambulancesOutOfService,
            Long globalNotificationFailures, int averageRecordedWaitMinutes,
            List<DoctorDayStatusResponse> doctorStatuses, List<QueueRow> queue
    ) {
    }
}
