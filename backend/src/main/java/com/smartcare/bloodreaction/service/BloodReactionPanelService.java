package com.smartcare.bloodreaction.service;

import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodreaction.domain.BloodReactionPanel;
import com.smartcare.bloodreaction.domain.BloodReactionPanelStatus;
import com.smartcare.bloodreaction.repository.BloodReactionPanelRepository;
import com.smartcare.bloodreaction.web.BloodReactionPanelDtos.PanelResponse;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.medicalrecord.storage.PrivateDocumentStorage;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

@Service
public class BloodReactionPanelService {
    private static final Set<Role> REVIEW_ROLES = Set.of(Role.LAB_TECHNICIAN, Role.BLOOD_BANK_STAFF, Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);
    private final BloodReactionPanelRepository panels;
    private final PatientRepository patients;
    private final UserAccountRepository users;
    private final PrivateDocumentStorage storage;
    private final BloodReactionModelClient model;
    private final AuditService audit;

    public BloodReactionPanelService(BloodReactionPanelRepository panels, PatientRepository patients,
                                     UserAccountRepository users, PrivateDocumentStorage storage,
                                     BloodReactionModelClient model, AuditService audit) {
        this.panels = panels; this.patients = patients; this.users = users;
        this.storage = storage; this.model = model; this.audit = audit;
    }

    @Transactional
    public PanelResponse submit(UUID userId, MultipartFile antiAFile, MultipartFile antiBFile, MultipartFile antiDFile) {
        Patient patient = patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Account was not found."));
        ImageData antiA = image(antiAFile, "Anti-A");
        ImageData antiB = image(antiBFile, "Anti-B");
        ImageData antiD = image(antiDFile, "Anti-D");

        var storedA = storage.store(patient.getId(), antiA.filename(), antiA.content());
        var storedB = storage.store(patient.getId(), antiB.filename(), antiB.content());
        var storedD = storage.store(patient.getId(), antiD.filename(), antiD.content());
        try {
            ensureImage(storedA.detectedContentType(), "Anti-A");
            ensureImage(storedB.detectedContentType(), "Anti-B");
            ensureImage(storedD.detectedContentType(), "Anti-D");
            var inference = model.analyze(
                    new BloodReactionModelClient.ImageInput(antiA.filename(), storedA.detectedContentType(), antiA.content()),
                    new BloodReactionModelClient.ImageInput(antiB.filename(), storedB.detectedContentType(), antiB.content()),
                    new BloodReactionModelClient.ImageInput(antiD.filename(), storedD.detectedContentType(), antiD.content()));
            boolean manualReview = inference.antiA().manualReviewRequired()
                    || inference.antiB().manualReviewRequired() || inference.antiD().manualReviewRequired();
            BloodGroup group = inference.bloodGroup() == null ? null : BloodGroup.valueOf(toEnumName(inference.bloodGroup()));
            BloodReactionPanel panel = panels.save(new BloodReactionPanel(patient, user,
                    storedA.storageKey(), storedB.storageKey(), storedD.storageKey(), antiA.filename(), antiB.filename(), antiD.filename(),
                    inference.antiA().agglutinationProbability(), inference.antiB().agglutinationProbability(), inference.antiD().agglutinationProbability(),
                    inference.antiA().confidence(), inference.antiB().confidence(), inference.antiD().confidence(),
                    inference.antiA().modelName(), inference.antiA().modelVersion(), group,
                    manualReview ? BloodReactionPanelStatus.MANUAL_REVIEW_REQUIRED : BloodReactionPanelStatus.PENDING_CLINICIAN_VERIFICATION,
                    inference.explanation()));
            audit.record("BLOOD_REACTION_PANEL_SUBMITTED", "BLOOD_REACTION_PANEL", panel.getId(), null);
            return toResponse(panel);
        } catch (RuntimeException exception) {
            storage.delete(storedA.storageKey()); storage.delete(storedB.storageKey()); storage.delete(storedD.storageKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<PanelResponse> mine(UUID userId) {
        Patient patient = patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
        return panels.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream().map(BloodReactionPanelService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PanelResponse> worklist(UUID userId, BloodReactionPanelStatus status) {
        requireReviewer(userId);
        List<BloodReactionPanel> rows = status == null ? panels.findAllByOrderByCreatedAtDesc() : panels.findAllByStatusOrderByCreatedAtAsc(status);
        return rows.stream().map(BloodReactionPanelService::toResponse).toList();
    }

    @Transactional
    public PanelResponse verify(UUID userId, UUID panelId, BloodGroup confirmedGroup, String note) {
        UserAccount reviewer = requireReviewer(userId);
        BloodReactionPanel panel = panels.findById(panelId).orElseThrow(() -> new NotFoundException("Reaction panel was not found."));
        try { panel.verify(reviewer, confirmedGroup, note == null ? "" : note.trim(), Instant.now()); }
        catch (IllegalStateException exception) { throw new IllegalArgumentException(exception.getMessage()); }
        audit.record("BLOOD_REACTION_PANEL_VERIFIED", "BLOOD_REACTION_PANEL", panelId, null);
        return toResponse(panel);
    }

    @Transactional
    public PanelResponse reject(UUID userId, UUID panelId, String reason) {
        UserAccount reviewer = requireReviewer(userId);
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A rejection reason is required.");
        BloodReactionPanel panel = panels.findById(panelId).orElseThrow(() -> new NotFoundException("Reaction panel was not found."));
        try { panel.reject(reviewer, reason.trim(), Instant.now()); }
        catch (IllegalStateException exception) { throw new IllegalArgumentException(exception.getMessage()); }
        audit.record("BLOOD_REACTION_PANEL_REJECTED", "BLOOD_REACTION_PANEL", panelId, null);
        return toResponse(panel);
    }

    private static ImageData image(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException(label + " image is required.");
        try { return new ImageData(safeFilename(file.getOriginalFilename()), file.getBytes()); }
        catch (IOException exception) { throw new IllegalArgumentException(label + " image could not be read.", exception); }
    }
    private static void ensureImage(String type, String label) {
        if (!"image/png".equals(type) && !"image/jpeg".equals(type)) throw new IllegalArgumentException(label + " must be a genuine JPG or PNG image.");
    }
    private static String safeFilename(String filename) {
        String safe = filename == null || filename.isBlank() ? "reaction-well.png" : filename.replaceAll("[\\r\\n\\\\/]", "_").trim();
        return safe.length() <= 180 ? safe : safe.substring(safe.length() - 180);
    }
    private static String toEnumName(String group) { return group.replace("+", "_POSITIVE").replace("-", "_NEGATIVE"); }
    private UserAccount requireReviewer(UUID userId) {
        UserAccount user = users.findById(userId).orElseThrow(() -> new AccessDeniedException("Account was not found."));
        if (user.getRoles().stream().noneMatch(REVIEW_ROLES::contains)) throw new AccessDeniedException("Authorized laboratory or blood-bank role required.");
        return user;
    }
    private static PanelResponse toResponse(BloodReactionPanel panel) {
        return new PanelResponse(panel.getId(), panel.getPatient().getPatientNumber(), panel.getAntiAFilename(), panel.getAntiBFilename(), panel.getAntiDFilename(),
                panel.getAntiAProbability(), panel.getAntiBProbability(), panel.getAntiDProbability(),
                panel.getAntiAConfidence(), panel.getAntiBConfidence(), panel.getAntiDConfidence(), panel.getModelName(),
                panel.getModelVersion(), panel.getSuggestedGroup(), panel.getStatus(), panel.getExplanation(), panel.getCreatedAt(),
                panel.getStatus() == BloodReactionPanelStatus.CLINICIAN_VERIFIED ? panel.getSuggestedGroup() : null,
                panel.getReviewedAt(), panel.getReviewNote(), panel.getRejectionReason());
    }
    private record ImageData(String filename, byte[] content) { }
}
