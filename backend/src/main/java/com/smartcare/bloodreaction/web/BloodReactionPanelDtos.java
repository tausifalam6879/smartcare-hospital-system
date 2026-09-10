package com.smartcare.bloodreaction.web;

import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodreaction.domain.BloodReactionPanelStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BloodReactionPanelDtos {
    private BloodReactionPanelDtos() { }
    public record PanelResponse(UUID id, String patientNumber, String antiAFilename, String antiBFilename, String antiDFilename,
                                BigDecimal antiAProbability, BigDecimal antiBProbability, BigDecimal antiDProbability,
                                BigDecimal antiAConfidence, BigDecimal antiBConfidence, BigDecimal antiDConfidence,
                                String modelName, String modelVersion, BloodGroup suggestedGroup,
                                BloodReactionPanelStatus status, String explanation, Instant createdAt,
                                BloodGroup confirmedGroup, Instant reviewedAt, String reviewNote, String rejectionReason) { }
    public record VerifyPanel(BloodGroup confirmedGroup, String note) { }
    public record RejectPanel(String reason) { }
}
