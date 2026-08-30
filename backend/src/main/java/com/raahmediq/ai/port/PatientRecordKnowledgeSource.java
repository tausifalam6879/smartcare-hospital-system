package com.raahmediq.ai.port;

import com.raahmediq.medicalrecord.domain.ClinicalVisit;
import com.raahmediq.medicalrecord.domain.MedicalDocument;

import java.util.List;
import java.util.UUID;

public interface PatientRecordKnowledgeSource {

    List<ClinicalSource> clinicalSources(UUID patientId);

    List<DocumentSource> documentSources(UUID patientId);

    byte[] loadDocument(UUID patientId, UUID documentId);

    record ClinicalSource(ClinicalVisit visit, String citationLabel, String content) {
    }

    record DocumentSource(MedicalDocument document, String citationLabel) {
    }
}
