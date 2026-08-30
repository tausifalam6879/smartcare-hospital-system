package com.raahmediq.bloodgroupai.service;

import com.raahmediq.audit.service.AuditService;
import com.raahmediq.auth.domain.Role;
import com.raahmediq.auth.domain.UserAccount;
import com.raahmediq.auth.repository.UserAccountRepository;
import com.raahmediq.bloodgroupai.domain.BloodGroupAnalysisStatus;
import com.raahmediq.bloodgroupai.domain.BloodGroupImageAnalysis;
import com.raahmediq.bloodgroupai.port.BloodSlideAnalyzer;
import com.raahmediq.bloodgroupai.repository.BloodGroupImageAnalysisRepository;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.AnalysisResponse;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.DownloadedImage;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.RecordObservations;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.RejectAnalysis;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.VerifyAnalysis;
import com.raahmediq.common.error.ConflictException;
import com.raahmediq.common.error.NotFoundException;
import com.raahmediq.hospital.domain.Hospital;
import com.raahmediq.hospital.repository.HospitalRepository;
import com.raahmediq.medicalrecord.storage.PrivateDocumentStorage;
import com.raahmediq.notification.domain.NotificationType;
import com.raahmediq.notification.service.NotificationService;
import com.raahmediq.patient.domain.Patient;
import com.raahmediq.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BloodGroupImageAnalysisService {
    private static final Set<Role> REVIEW_ROLES = Set.of(Role.LAB_TECHNICIAN, Role.BLOOD_BANK_STAFF,
            Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);
    private static final Set<Role> OBSERVATION_ROLES = Set.of(Role.LAB_TECHNICIAN, Role.BLOOD_BANK_STAFF);

    private final BloodGroupImageAnalysisRepository analyses;
    private final PatientRepository patients;
    private final HospitalRepository hospitals;
    private final UserAccountRepository users;
    private final PrivateDocumentStorage storage;
    private final BloodSlideAnalyzer analyzer;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public BloodGroupImageAnalysisService(BloodGroupImageAnalysisRepository analyses, PatientRepository patients,
                                          HospitalRepository hospitals, UserAccountRepository users,
                                          PrivateDocumentStorage storage, BloodSlideAnalyzer analyzer,
                                          NotificationService notifications, AuditService audit, Clock clock) {
        this.analyses = analyses;
        this.patients = patients;
        this.hospitals = hospitals;
        this.users = users;
        this.storage = storage;
        this.analyzer = analyzer;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public AnalysisResponse submit(UUID userId, UUID hospitalId, boolean safetyAcknowledged, MultipartFile file) {
        if (!safetyAcknowledged) {
            throw new IllegalArgumentException("Acknowledge that this image cannot replace validated laboratory testing.");
        }
        Patient patient = requirePatient(userId);
        UserAccount submitter = requireUser(userId);
        Hospital hospital = hospitals.findById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital was not found."));
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("The uploaded image could not be read.", exception);
        }
        validateImage(content);
        var stored = storage.store(patient.getId(), safeFilename(file.getOriginalFilename()), content);
        if (!stored.detectedContentType().equals("image/jpeg") && !stored.detectedContentType().equals("image/png")) {
            storage.delete(stored.storageKey());
            throw new IllegalArgumentException("Only a genuine JPG or PNG blood-slide image is accepted.");
        }
        try {
            var attempt = analyzer.analyze(content, stored.detectedContentType());
            BloodGroupImageAnalysis analysis = analyses.save(new BloodGroupImageAnalysis(patient, hospital, submitter,
                    stored.storageKey(), safeFilename(file.getOriginalFilename()), stored.detectedContentType(),
                    stored.sizeBytes(), stored.sha256(), attempt.status(), attempt.suggestedGroup(),
                    attempt.confidence()));
            audit.record("BLOOD_GROUP_IMAGE_SUBMITTED", "BLOOD_GROUP_IMAGE_ANALYSIS", analysis.getId(), hospitalId);
            notifications.notifyPatient(patient, hospitalId, NotificationType.BLOOD_GROUP_ANALYSIS_SUBMITTED,
                    analysis.getId(), "submitted", "Blood-slide image received",
                    "Authorized laboratory review is required. No blood group has been confirmed from the image.");
            return toResponse(analysis);
        } catch (RuntimeException exception) {
            storage.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        return analyses.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(BloodGroupImageAnalysisService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> worklist(UUID userId, UUID hospitalId, BloodGroupAnalysisStatus status) {
        requireRole(userId, REVIEW_ROLES);
        if (!hospitals.existsById(hospitalId)) throw new NotFoundException("Hospital was not found.");
        List<BloodGroupImageAnalysis> rows = status == null
                ? analyses.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId)
                : analyses.findAllByHospitalIdAndStatusOrderByCreatedAtAsc(hospitalId, status);
        return rows.stream().map(BloodGroupImageAnalysisService::toResponse).toList();
    }

    @Transactional
    public DownloadedImage download(UUID userId, UUID analysisId) {
        UserAccount actor = requireUser(userId);
        BloodGroupImageAnalysis analysis = requireAnalysis(analysisId);
        boolean owner = analysis.getPatient().getUser().getId().equals(userId);
        boolean reviewer = actor.getRoles().stream().anyMatch(REVIEW_ROLES::contains);
        if (!owner && !reviewer) throw new AccessDeniedException("This image belongs to another patient.");
        audit.record("BLOOD_GROUP_IMAGE_VIEWED", "BLOOD_GROUP_IMAGE_ANALYSIS", analysisId,
                analysis.getHospital().getId());
        return new DownloadedImage(analysis.getOriginalFilename(), analysis.getContentType(),
                storage.load(analysis.getStorageKey()));
    }

    @Transactional
    public AnalysisResponse recordObservations(UUID userId, UUID analysisId, RecordObservations input) {
        UserAccount actor = requireRole(userId, OBSERVATION_ROLES);
        BloodGroupImageAnalysis analysis = requireAnalysisForUpdate(analysisId);
        transition(() -> analysis.recordObservations(input.antiAReactive(), input.antiBReactive(),
                input.antiDReactive(), actor, input.note(), clock.instant()));
        audit.record("BLOOD_GROUP_OBSERVATIONS_RECORDED", "BLOOD_GROUP_IMAGE_ANALYSIS", analysisId,
                analysis.getHospital().getId());
        notifyUpdate(analysis, "observed", "Laboratory observations recorded",
                "A preliminary reaction pattern was recorded. Independent verification is still required.");
        return toResponse(analysis);
    }

    @Transactional
    public AnalysisResponse verify(UUID userId, UUID analysisId, VerifyAnalysis input) {
        UserAccount actor = requireRole(userId, REVIEW_ROLES);
        BloodGroupImageAnalysis analysis = requireAnalysisForUpdate(analysisId);
        transition(() -> analysis.verify(input.confirmedGroup(), actor, clock.instant()));
        audit.record("BLOOD_GROUP_ANALYSIS_VERIFIED", "BLOOD_GROUP_IMAGE_ANALYSIS", analysisId,
                analysis.getHospital().getId());
        notifyUpdate(analysis, "verified", "Blood group laboratory-verified",
                "Authorized staff independently verified the recorded reaction pattern as "
                        + input.confirmedGroup() + ". Reconfirm with the treating laboratory before clinical use.");
        return toResponse(analysis);
    }

    @Transactional
    public AnalysisResponse reject(UUID userId, UUID analysisId, RejectAnalysis input) {
        UserAccount actor = requireRole(userId, REVIEW_ROLES);
        BloodGroupImageAnalysis analysis = requireAnalysisForUpdate(analysisId);
        transition(() -> analysis.reject(actor, input.reason(), clock.instant()));
        audit.record("BLOOD_GROUP_ANALYSIS_REJECTED", "BLOOD_GROUP_IMAGE_ANALYSIS", analysisId,
                analysis.getHospital().getId());
        notifyUpdate(analysis, "rejected", "Blood-slide image needs replacement",
                "The image was not accepted for review. Reason: " + input.reason());
        return toResponse(analysis);
    }

    private void notifyUpdate(BloodGroupImageAnalysis analysis, String key, String title, String message) {
        notifications.notifyPatient(analysis.getPatient(), analysis.getHospital().getId(),
                NotificationType.BLOOD_GROUP_ANALYSIS_UPDATED, analysis.getId(), key, title, message);
    }

    private BloodGroupImageAnalysis requireAnalysis(UUID id) {
        return analyses.findById(id).orElseThrow(() -> new NotFoundException("Blood-group analysis was not found."));
    }

    private BloodGroupImageAnalysis requireAnalysisForUpdate(UUID id) {
        return analyses.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Blood-group analysis was not found."));
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }

    private UserAccount requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new AccessDeniedException("Account was not found."));
    }

    private UserAccount requireRole(UUID id, Set<Role> roles) {
        UserAccount user = requireUser(id);
        if (user.getRoles().stream().noneMatch(roles::contains)) {
            throw new AccessDeniedException("Authorized laboratory or blood-bank role required.");
        }
        return user;
    }

    private static String safeFilename(String value) {
        String safe = value == null || value.isBlank() ? "blood-slide-image" : value
                .replaceAll("[\\r\\n\\\\/]", "_").trim();
        return safe.length() <= 180 ? safe : safe.substring(safe.length() - 180);
    }

    private static void validateImage(byte[] content) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("Blood-slide image is empty.");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("The upload is not a decodable JPG or PNG image.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (width < 32 || height < 32) {
                    throw new IllegalArgumentException("Blood-slide image must be at least 32 by 32 pixels.");
                }
                if (pixels > 25_000_000L) {
                    throw new IllegalArgumentException("Blood-slide image dimensions are too large.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("The blood-slide image could not be decoded safely.", exception);
        }
    }

    private static AnalysisResponse toResponse(BloodGroupImageAnalysis item) {
        return new AnalysisResponse(item.getId(), item.getPatient().getPatientNumber(),
                item.getHospital().getId(), item.getHospital().getName(), item.getOriginalFilename(),
                item.getContentType(), item.getSizeBytes(), item.getStatus(), item.getModelInferenceStatus(),
                item.getModelSuggestedGroup(), item.getModelConfidence(), item.getAntiAReactive(),
                item.getAntiBReactive(), item.getAntiDReactive(), item.getPreliminaryGroup(),
                item.getVerifiedGroup(), item.getObservationNote(), item.getObservedAt(), item.getVerifiedAt(),
                item.getRejectionReason(), item.getRejectedAt(), item.getCreatedAt());
    }

    private static void transition(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }
}
