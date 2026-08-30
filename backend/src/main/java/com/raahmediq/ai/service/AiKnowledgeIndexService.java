package com.raahmediq.ai.service;

import com.raahmediq.ai.domain.DocumentChunk;
import com.raahmediq.ai.domain.KnowledgeIndexState;
import com.raahmediq.ai.domain.KnowledgeIndexStatus;
import com.raahmediq.ai.domain.KnowledgeSourceType;
import com.raahmediq.ai.port.PatientRecordKnowledgeSource;
import com.raahmediq.ai.repository.DocumentChunkRepository;
import com.raahmediq.ai.repository.KnowledgeIndexStateRepository;
import com.raahmediq.patient.domain.Patient;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiKnowledgeIndexService {
    private static final int CHUNK_SIZE = 900;
    private static final int CHUNK_OVERLAP = 120;

    private final PatientRecordKnowledgeSource sources;
    private final KnowledgeIndexStateRepository states;
    private final DocumentChunkRepository chunks;
    private final MedicalDocumentTextExtractor extractor;
    private final LocalHashEmbedding embeddings;
    private final Clock clock;

    public AiKnowledgeIndexService(PatientRecordKnowledgeSource sources, KnowledgeIndexStateRepository states,
                                   DocumentChunkRepository chunks, MedicalDocumentTextExtractor extractor,
                                   LocalHashEmbedding embeddings, Clock clock) {
        this.sources = sources;
        this.states = states;
        this.chunks = chunks;
        this.extractor = extractor;
        this.embeddings = embeddings;
        this.clock = clock;
    }

    public void ensureIndexed(Patient patient) {
        sources.clinicalSources(patient.getId()).forEach(source -> indexClinical(patient, source));
        sources.documentSources(patient.getId()).forEach(source -> indexDocument(patient, source));
    }

    private void indexClinical(Patient patient, PatientRecordKnowledgeSource.ClinicalSource source) {
        String hash = embeddings.sha256(source.content());
        if (current(patient, KnowledgeSourceType.CLINICAL_VISIT, source.visit().getId(), hash)) return;
        chunks.deleteAllByPatientIdAndSourceTypeAndSourceKey(patient.getId(), KnowledgeSourceType.CLINICAL_VISIT,
                source.visit().getId());
        List<String> parts = chunk(source.content());
        for (int index = 0; index < parts.size(); index++) {
            String content = parts.get(index);
            chunks.save(new DocumentChunk(patient, source.visit().getHospital(), KnowledgeSourceType.CLINICAL_VISIT,
                    source.visit().getId(), null, source.visit(), index, null, source.citationLabel(), content,
                    LocalHashEmbedding.MODEL, embeddings.embed(source.citationLabel() + "\n" + content),
                    embeddings.sha256(content), true));
        }
        complete(patient, KnowledgeSourceType.CLINICAL_VISIT, source.visit().getId(), hash,
                KnowledgeIndexStatus.INDEXED, null);
    }

    private void indexDocument(Patient patient, PatientRecordKnowledgeSource.DocumentSource source) {
        var document = source.document();
        if (current(patient, KnowledgeSourceType.MEDICAL_DOCUMENT, document.getId(), document.getSha256())) return;
        chunks.deleteAllByPatientIdAndSourceTypeAndSourceKey(patient.getId(), KnowledgeSourceType.MEDICAL_DOCUMENT,
                document.getId());
        var extraction = extractor.extract(document.getContentType(), sources.loadDocument(patient.getId(), document.getId()));
        int index = 0;
        for (var page : extraction.pages()) {
            for (String part : chunk(page.text())) {
                String content = "Document type: " + document.getDocumentType().name().replace('_', ' ')
                        + "\nDocument date: " + document.getDocumentDate() + "\n" + part;
                chunks.save(new DocumentChunk(patient, document.getHospital(), KnowledgeSourceType.MEDICAL_DOCUMENT,
                        document.getId(), document, null, index++, page.pageNumber(), source.citationLabel(), content,
                        LocalHashEmbedding.MODEL, embeddings.embed(source.citationLabel() + "\n" + content),
                        embeddings.sha256(content), true));
            }
        }
        KnowledgeIndexStatus status;
        if (!extraction.pages().isEmpty()) {
            status = KnowledgeIndexStatus.INDEXED;
        } else if ("OCR_REQUIRED".equals(extraction.errorCode())) {
            status = KnowledgeIndexStatus.OCR_REQUIRED;
        } else if ("NO_EXTRACTABLE_TEXT".equals(extraction.errorCode())) {
            status = KnowledgeIndexStatus.NO_TEXT;
        } else {
            status = KnowledgeIndexStatus.FAILED;
        }
        if (index == 0) {
            String metadata = "Document type: " + document.getDocumentType().name().replace('_', ' ')
                    + "\nFilename: " + document.getOriginalFilename()
                    + "\nDocument date: " + document.getDocumentDate()
                    + "\nHospital: " + document.getHospital().getName()
                    + "\nContent extraction status: " + status.name().replace('_', ' ')
                    + (document.getDescription() == null ? "" : "\nDescription: " + document.getDescription());
            chunks.save(new DocumentChunk(patient, document.getHospital(), KnowledgeSourceType.MEDICAL_DOCUMENT,
                    document.getId(), document, null, 0, null, source.citationLabel(), metadata,
                    LocalHashEmbedding.MODEL, embeddings.embed(source.citationLabel() + "\n" + metadata),
                    embeddings.sha256(metadata), false));
        }
        complete(patient, KnowledgeSourceType.MEDICAL_DOCUMENT, document.getId(), document.getSha256(), status,
                extraction.errorCode());
    }

    private boolean current(Patient patient, KnowledgeSourceType type, java.util.UUID key, String hash) {
        return states.findByPatientIdAndSourceTypeAndSourceKey(patient.getId(), type, key)
                .map(state -> hash.equals(state.getSourceHash()))
                .orElse(false);
    }

    private void complete(Patient patient, KnowledgeSourceType type, java.util.UUID key, String hash,
                          KnowledgeIndexStatus status, String errorCode) {
        KnowledgeIndexState state = states.findByPatientIdAndSourceTypeAndSourceKey(patient.getId(), type, key)
                .orElseGet(() -> new KnowledgeIndexState(patient, type, key));
        state.complete(hash, status, errorCode, clock.instant());
        states.save(state);
    }

    private List<String> chunk(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int end = Math.min(value.length(), start + CHUNK_SIZE);
            if (end < value.length()) {
                int boundary = value.lastIndexOf(' ', end);
                if (boundary > start + CHUNK_SIZE / 2) end = boundary;
            }
            result.add(value.substring(start, end).trim());
            if (end >= value.length()) break;
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return result;
    }
}
