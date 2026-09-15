package com.smartcare.medicalrecord.web;

import com.smartcare.medicalrecord.domain.AllergySeverity;
import com.smartcare.medicalrecord.domain.AllergyStatus;
import com.smartcare.medicalrecord.domain.DocumentType;
import com.smartcare.medicalrecord.domain.DocumentVerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MedicalRecordDtos {
    private MedicalRecordDtos() {
    }

    public record VisitRecordRequest(
            @NotNull UUID appointmentId,
            @Size(max = 2000) String symptoms,
            @NotBlank @Size(max = 2000) String diagnosis,
            @Size(max = 4000) String doctorNotes,
            @Size(max = 4000) String dischargeSummary,
            @Size(max = 2000) String followUpRecommendation,
            @Size(max = 1200) String prescriptionInstructions,
            @Valid @Size(max = 30) List<PrescriptionItemRequest> medicines,
            @Valid @Size(max = 30) List<AllergyRequest> allergies,
            LocalDate followUpDate,
            Boolean medicationReminderEnabled
    ) {
    }

    public record PrescriptionItemRequest(
            @NotBlank @Size(max = 180) String medicineName,
            @NotBlank @Size(max = 100) String dosage,
            @NotBlank @Size(max = 140) String frequency,
            @NotBlank @Size(max = 140) String duration,
            @Size(max = 80) String route,
            @Size(max = 500) String instructions
    ) {
    }

    public record AllergyRequest(
            @NotBlank @Size(max = 180) String substance,
            @Size(max = 500) String reaction,
            @NotNull AllergySeverity severity
    ) {
    }

    public record MedicalRecordResponse(String patientNumber, Instant generatedAt, List<VisitResponse> visits,
                                        List<AllergyResponse> allergies, List<DocumentResponse> documents) {
    }

    public record VisitResponse(UUID id, UUID appointmentId, LocalDate visitDate, String hospitalName,
                                String doctorName, String specialization, String symptoms, String diagnosis,
                                String doctorNotes, String dischargeSummary, String followUpRecommendation,
                                Instant finalizedAt, PrescriptionResponse prescription) {
    }

    public record PrescriptionResponse(UUID id, String generalInstructions, Instant prescribedAt,
                                       List<PrescriptionItemResponse> medicines) {
    }

    public record PrescriptionItemResponse(String medicineName, String dosage, String frequency,
                                           String duration, String route, String instructions) {
    }

    public record AllergyResponse(UUID id, String substance, String reaction, AllergySeverity severity,
                                  AllergyStatus status, String recordedByDoctor, Instant recordedAt) {
    }

    public record DocumentResponse(UUID id, UUID hospitalId, String hospitalName, DocumentType documentType,
                                   String originalFilename, String contentType, long sizeBytes,
                                   LocalDate documentDate, String description,
                                   DocumentVerificationStatus verificationStatus, Instant uploadedAt,
                                   String contentPath, String assistantReadiness) {
    }

    public record DownloadedDocument(String filename, String contentType, byte[] content) {
    }
}
