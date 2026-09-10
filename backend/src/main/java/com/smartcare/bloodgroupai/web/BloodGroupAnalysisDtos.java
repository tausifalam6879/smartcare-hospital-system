package com.smartcare.bloodgroupai.web;

import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodgroupai.domain.BloodGroupAnalysisStatus;
import com.smartcare.bloodgroupai.domain.ModelInferenceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BloodGroupAnalysisDtos {
    private BloodGroupAnalysisDtos() {
    }

    public record RecordObservations(@NotNull Boolean antiAReactive, @NotNull Boolean antiBReactive,
                                     @NotNull Boolean antiDReactive, @Size(max = 500) String note) {
    }

    public record VerifyAnalysis(@NotNull BloodGroup confirmedGroup) {
    }

    public record RejectAnalysis(@NotBlank @Size(max = 300) String reason) {
    }

    public record AnalysisResponse(UUID id, String patientNumber, UUID hospitalId, String hospitalName,
                                   String originalFilename, String contentType, long sizeBytes,
                                   BloodGroupAnalysisStatus status, ModelInferenceStatus modelInferenceStatus,
                                   BloodGroup modelSuggestedGroup, BigDecimal modelConfidence,
                                   Boolean antiAReactive, Boolean antiBReactive, Boolean antiDReactive,
                                   BloodGroup preliminaryGroup, BloodGroup verifiedGroup,
                                   String observationNote, Instant observedAt, Instant verifiedAt,
                                   String rejectionReason, Instant rejectedAt, Instant createdAt) {
    }

    public record DownloadedImage(String filename, String contentType, byte[] content) {
    }
}
