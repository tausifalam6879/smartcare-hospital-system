package com.smartcare.medicalrecord.service;

import com.smartcare.ai.port.PatientRecordKnowledgeSource;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.medicalrecord.domain.Prescription;
import com.smartcare.medicalrecord.repository.ClinicalVisitRepository;
import com.smartcare.medicalrecord.repository.MedicalDocumentRepository;
import com.smartcare.medicalrecord.repository.PatientAllergyRepository;
import com.smartcare.medicalrecord.repository.PrescriptionItemRepository;
import com.smartcare.medicalrecord.repository.PrescriptionRepository;
import com.smartcare.medicalrecord.storage.PrivateDocumentStorage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PatientRecordKnowledgeSourceAdapter implements PatientRecordKnowledgeSource {
    private final ClinicalVisitRepository visits;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionItemRepository items;
    private final PatientAllergyRepository allergies;
    private final MedicalDocumentRepository documents;
    private final PrivateDocumentStorage storage;

    public PatientRecordKnowledgeSourceAdapter(ClinicalVisitRepository visits, PrescriptionRepository prescriptions,
                                               PrescriptionItemRepository items, PatientAllergyRepository allergies,
                                               MedicalDocumentRepository documents, PrivateDocumentStorage storage) {
        this.visits = visits;
        this.prescriptions = prescriptions;
        this.items = items;
        this.allergies = allergies;
        this.documents = documents;
        this.storage = storage;
    }

    @Override
    public List<ClinicalSource> clinicalSources(UUID patientId) {
        return visits.findAllByPatientIdOrderByVisitDateDescCreatedAtDesc(patientId).stream()
                .map(visit -> {
                    String label = "Consultation with " + visit.getDoctor().getName() + " on " + visit.getVisitDate();
                    List<String> facts = new ArrayList<>();
                    facts.add("Consultation date: " + visit.getVisitDate());
                    facts.add("Doctor: " + visit.getDoctor().getName() + " (" + visit.getDoctor().getSpecialization() + ")");
                    facts.add("Hospital: " + visit.getHospital().getName());
                    add(facts, "Clinician-documented symptoms", visit.getSymptoms());
                    add(facts, "Diagnosis entered by doctor", visit.getDiagnosis());
                    add(facts, "Doctor notes", visit.getDoctorNotes());
                    add(facts, "Discharge summary", visit.getDischargeSummary());
                    add(facts, "Follow-up recommendation", visit.getFollowUpRecommendation());
                    prescriptions.findByClinicalVisitId(visit.getId()).ifPresent(prescription ->
                            addPrescription(facts, prescription));
                    allergies.findAllByAppointmentIdOrderByRecordedAtAsc(visit.getAppointment().getId()).forEach(allergy ->
                            facts.add("Allergy recorded: " + allergy.getSubstance() + " — severity " + allergy.getSeverity()
                                    + optional("; reaction ", allergy.getReaction())));
                    return new ClinicalSource(visit, label, String.join("\n", facts));
                }).toList();
    }

    @Override
    public List<DocumentSource> documentSources(UUID patientId) {
        return documents.findAllByPatientIdOrderByDocumentDateDescCreatedAtDesc(patientId).stream()
                .map(document -> new DocumentSource(document,
                        document.getDocumentType().name().replace('_', ' ') + " — " + document.getOriginalFilename()
                                + " (" + document.getDocumentDate() + ", "
                                + document.getVerificationStatus().name().replace('_', ' ').toLowerCase() + ")"))
                .toList();
    }

    @Override
    public byte[] loadDocument(UUID patientId, UUID documentId) {
        var document = documents.findByIdAndPatientId(documentId, patientId)
                .orElseThrow(() -> new NotFoundException("Medical document not found."));
        return storage.load(document.getStorageKey());
    }

    private void addPrescription(List<String> facts, Prescription prescription) {
        add(facts, "Prescription instructions", prescription.getGeneralInstructions());
        items.findAllByPrescriptionIdOrderByItemOrderAsc(prescription.getId()).forEach(item ->
                facts.add("Prescribed medicine: " + item.getMedicineName() + "; dosage " + item.getDosage()
                        + "; frequency " + item.getFrequency() + "; duration " + item.getDurationText()
                        + optional("; route ", item.getRoute()) + optional("; instructions ", item.getInstructions())));
    }

    private static void add(List<String> facts, String label, String value) {
        if (value != null && !value.isBlank()) facts.add(label + ": " + value.trim());
    }

    private static String optional(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value.trim();
    }
}
