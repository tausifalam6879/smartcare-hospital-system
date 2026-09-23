package com.smartcare.medicalrecord.service;

import com.smartcare.common.time.HospitalDate;

import com.smartcare.ai.domain.KnowledgeSourceType;
import com.smartcare.ai.repository.KnowledgeIndexStateRepository;
import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.repository.AppointmentRepository;
import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.followup.service.CareFollowUpService;
import com.smartcare.medicalrecord.domain.ClinicalVisit;
import com.smartcare.medicalrecord.domain.DocumentType;
import com.smartcare.medicalrecord.domain.MedicalDocument;
import com.smartcare.medicalrecord.domain.PatientAllergy;
import com.smartcare.medicalrecord.domain.Prescription;
import com.smartcare.medicalrecord.domain.PrescriptionItem;
import com.smartcare.medicalrecord.repository.ClinicalVisitRepository;
import com.smartcare.medicalrecord.repository.MedicalDocumentRepository;
import com.smartcare.medicalrecord.repository.PatientAllergyRepository;
import com.smartcare.medicalrecord.repository.PrescriptionItemRepository;
import com.smartcare.medicalrecord.repository.PrescriptionRepository;
import com.smartcare.medicalrecord.storage.PrivateDocumentStorage;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.AllergyResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.DocumentResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.DownloadedDocument;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.MedicalRecordResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.PrescriptionItemResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.PrescriptionResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.VisitRecordRequest;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.VisitResponse;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MedicalRecordService {

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final UserAccountRepository users;
    private final HospitalRepository hospitals;
    private final ClinicalVisitRepository visits;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository prescriptionItems;
    private final PatientAllergyRepository allergies;
    private final MedicalDocumentRepository documents;
    private final PrivateDocumentStorage storage;
    private final AuditService audit;
    private final CareFollowUpService followUps;
    private final NotificationService notifications;
    private final KnowledgeIndexStateRepository knowledgeIndexStates;
    private final Clock clock;

    public MedicalRecordService(PatientRepository patients, DoctorRepository doctors,
                                AppointmentRepository appointments, UserAccountRepository users,
                                HospitalRepository hospitals, ClinicalVisitRepository visits,
                                PrescriptionRepository prescriptions, PrescriptionItemRepository prescriptionItems,
                                PatientAllergyRepository allergies, MedicalDocumentRepository documents,
                                PrivateDocumentStorage storage, AuditService audit, CareFollowUpService followUps,
                                NotificationService notifications,
                                KnowledgeIndexStateRepository knowledgeIndexStates, Clock clock) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.users = users;
        this.hospitals = hospitals;
        this.visits = visits;
        this.prescriptions = prescriptions;
        this.prescriptionItems = prescriptionItems;
        this.allergies = allergies;
        this.documents = documents;
        this.storage = storage;
        this.audit = audit;
        this.followUps = followUps;
        this.notifications = notifications;
        this.knowledgeIndexStates = knowledgeIndexStates;
        this.clock = clock;
    }

    @Transactional
    public MedicalRecordResponse mine(UUID userId) {
        Patient patient = requirePatient(userId);
        audit.record("MEDICAL_RECORD_VIEWED", "PATIENT", patient.getId(), null);
        return response(patient);
    }

    @Transactional
    public MedicalRecordResponse forAppointment(UUID doctorUserId, UUID appointmentId) {
        Doctor doctor = requireLinkedDoctor(doctorUserId);
        Appointment appointment = requireAppointment(appointmentId);
        requireAssignedDoctor(doctor, appointment);
        audit.record("PATIENT_RECORD_VIEWED_BY_DOCTOR", "PATIENT", appointment.getPatient().getId(),
                appointment.getHospital().getId());
        return response(appointment.getPatient());
    }

    @Transactional
    public VisitResponse recordVisit(UUID doctorUserId, VisitRecordRequest request) {
        Doctor doctor = requireLinkedDoctor(doctorUserId);
        UserAccount recorder = users.findById(doctorUserId)
                .orElseThrow(() -> new AccessDeniedException("Doctor account is unavailable."));
        Appointment appointment = requireAppointment(request.appointmentId());
        requireAssignedDoctor(doctor, appointment);
        if (appointment.getStatus() != AppointmentStatus.IN_CONSULTATION
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException("Clinical notes can be finalized only during or after consultation.");
        }
        if (visits.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new ConflictException("A finalized clinical record already exists for this appointment.");
        }
        var now = clock.instant();
        ClinicalVisit visit = visits.save(new ClinicalVisit(appointment, recorder, clean(request.symptoms()),
                request.diagnosis().trim(), clean(request.doctorNotes()), clean(request.dischargeSummary()),
                clean(request.followUpRecommendation()), now));
        boolean hasPrescription = request.medicines() != null && !request.medicines().isEmpty();
        if (hasPrescription) {
            Prescription prescription = prescriptions.save(new Prescription(visit,
                    clean(request.prescriptionInstructions()), now));
            int order = 1;
            for (var item : request.medicines()) {
                prescriptionItems.save(new PrescriptionItem(prescription, order++, item.medicineName().trim(),
                        item.dosage().trim(), item.frequency().trim(), item.duration().trim(), clean(item.route()),
                        clean(item.instructions())));
            }
        }
        if (request.allergies() != null) {
            for (var allergy : request.allergies()) {
                allergies.save(new PatientAllergy(appointment, recorder, allergy.substance().trim(),
                        clean(allergy.reaction()), allergy.severity(), now));
            }
        }
        if (request.followUpDate() != null) {
            followUps.schedule(visit, request.followUpDate(), hasPrescription
                    && Boolean.TRUE.equals(request.medicationReminderEnabled()));
        }
        if (appointment.getStatus() == AppointmentStatus.IN_CONSULTATION) {
            appointment.complete(now);
            notifications.notifyAppointment(appointment, NotificationType.VISIT_COMPLETED, "record-finalized",
                    "Consultation completed",
                    "Your clinician finalized the visit record. Open Records for the documented care plan.");
        }
        audit.record("CLINICAL_VISIT_FINALIZED", "CLINICAL_VISIT", visit.getId(), appointment.getHospital().getId());
        return toResponse(visit);
    }

    @Transactional
    public DocumentResponse upload(UUID userId, UUID hospitalId, DocumentType documentType,
                                   LocalDate documentDate, String description, MultipartFile file) {
        Patient patient = requirePatient(userId);
        UserAccount uploader = users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Patient account is unavailable."));
        Hospital hospital = hospitals.findById(hospitalId).filter(Hospital::isActive)
                .orElseThrow(() -> new NotFoundException("Hospital was not found."));
        if (documentDate.isAfter(HospitalDate.today(clock, hospital))) {
            throw new IllegalArgumentException("Document date cannot be in the future.");
        }
        String filename = safeFilename(file.getOriginalFilename());
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Document could not be read.");
        }
        PrivateDocumentStorage.StoredDocument stored = storage.store(patient.getId(), filename, content);
        try {
            MedicalDocument document = documents.save(new MedicalDocument(patient, hospital, uploader, documentType,
                    filename, stored.detectedContentType(), stored.sizeBytes(), stored.storageKey(), stored.sha256(),
                    documentDate, clean(description)));
            audit.record("MEDICAL_DOCUMENT_UPLOADED", "MEDICAL_DOCUMENT", document.getId(), hospitalId);
            return toResponse(document, patient);
        } catch (RuntimeException exception) {
            storage.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional
    public DownloadedDocument download(UUID userId, UUID documentId) {
        MedicalDocument document = documents.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Medical document was not found."));
        boolean patientOwns = patients.findByUserId(userId)
                .map(patient -> patient.getId().equals(document.getPatient().getId())).orElse(false);
        boolean authorizedDoctor = doctors.findByLinkedUserId(userId)
                .map(doctor -> appointments.existsByPatientIdAndDoctorId(document.getPatient().getId(), doctor.getId()))
                .orElse(false);
        if (!patientOwns && !authorizedDoctor) throw new AccessDeniedException("Medical document access is denied.");
        audit.record("MEDICAL_DOCUMENT_DOWNLOADED", "MEDICAL_DOCUMENT", document.getId(),
                document.getHospital().getId());
        return new DownloadedDocument(document.getOriginalFilename(), document.getContentType(),
                storage.load(document.getStorageKey()));
    }

    private MedicalRecordResponse response(Patient patient) {
        return new MedicalRecordResponse(patient.getPatientNumber(), clock.instant(),
                visits.findAllByPatientIdOrderByVisitDateDescCreatedAtDesc(patient.getId()).stream()
                        .map(this::toResponse).toList(),
                allergies.findAllByPatientIdOrderByRecordedAtDesc(patient.getId()).stream()
                        .map(MedicalRecordService::toResponse).toList(),
                documents.findAllByPatientIdOrderByDocumentDateDescCreatedAtDesc(patient.getId()).stream()
                        .map(document -> toResponse(document, patient)).toList());
    }

    private VisitResponse toResponse(ClinicalVisit visit) {
        PrescriptionResponse prescription = prescriptions.findByClinicalVisitId(visit.getId())
                .map(item -> new PrescriptionResponse(item.getId(), item.getGeneralInstructions(),
                        item.getPrescribedAt(), prescriptionItems
                        .findAllByPrescriptionIdOrderByItemOrderAsc(item.getId()).stream()
                        .map(MedicalRecordService::toResponse).toList())).orElse(null);
        return new VisitResponse(visit.getId(), visit.getAppointment().getId(), visit.getVisitDate(),
                visit.getHospital().getName(), visit.getDoctor().getName(), visit.getDoctor().getSpecialization(),
                visit.getSymptoms(), visit.getDiagnosis(), visit.getDoctorNotes(), visit.getDischargeSummary(),
                visit.getFollowUpRecommendation(), visit.getFinalizedAt(), prescription);
    }

    private static PrescriptionItemResponse toResponse(PrescriptionItem item) {
        return new PrescriptionItemResponse(item.getMedicineName(), item.getDosage(), item.getFrequency(),
                item.getDurationText(), item.getRoute(), item.getInstructions());
    }

    private static AllergyResponse toResponse(PatientAllergy allergy) {
        return new AllergyResponse(allergy.getId(), allergy.getSubstance(), allergy.getReaction(),
                allergy.getSeverity(), allergy.getStatus(), allergy.getDoctor().getName(), allergy.getRecordedAt());
    }

    private DocumentResponse toResponse(MedicalDocument document, Patient patient) {
        String assistantReadiness = knowledgeIndexStates
                .findByPatientIdAndSourceTypeAndSourceKey(patient.getId(), KnowledgeSourceType.MEDICAL_DOCUMENT,
                        document.getId())
                .map(state -> state.getStatus().name())
                .orElseGet(() -> "application/pdf".equals(document.getContentType())
                        ? "READY_FOR_TEXT_CHECK" : "OCR_REQUIRED");
        return new DocumentResponse(document.getId(), document.getHospital().getId(), document.getHospital().getName(),
                document.getDocumentType(), document.getOriginalFilename(), document.getContentType(),
                document.getSizeBytes(), document.getDocumentDate(), document.getDescription(),
                document.getVerificationStatus(), document.getCreatedAt(),
                "/api/v1/medical-records/documents/" + document.getId() + "/content", assistantReadiness);
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }

    private Doctor requireLinkedDoctor(UUID userId) {
        return doctors.findByLinkedUserId(userId).filter(Doctor::isActive)
                .orElseThrow(() -> new AccessDeniedException("A linked active doctor profile is required."));
    }

    private Appointment requireAppointment(UUID id) {
        return appointments.findById(id).orElseThrow(() -> new NotFoundException("Appointment was not found."));
    }

    private static void requireAssignedDoctor(Doctor doctor, Appointment appointment) {
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new AccessDeniedException("This appointment is assigned to another doctor.");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeFilename(String value) {
        String filename = value == null || value.isBlank() ? "medical-document" : value;
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}<>:\"|?*]", "").trim();
        if (filename.length() > 240) filename = filename.substring(filename.length() - 240);
        return filename.isBlank() ? "medical-document" : filename;
    }
}
