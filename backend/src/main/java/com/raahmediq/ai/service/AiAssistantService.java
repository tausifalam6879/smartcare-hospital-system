package com.raahmediq.ai.service;

import com.raahmediq.ai.domain.AiConversation;
import com.raahmediq.ai.domain.AiMessage;
import com.raahmediq.ai.domain.AiMessageCitation;
import com.raahmediq.ai.domain.AiMessageRole;
import com.raahmediq.ai.domain.AiSafetyClass;
import com.raahmediq.ai.domain.KnowledgeSourceType;
import com.raahmediq.ai.repository.AiConversationRepository;
import com.raahmediq.ai.repository.AiMessageCitationRepository;
import com.raahmediq.ai.repository.AiMessageRepository;
import com.raahmediq.ai.service.AiRetrievalService.RetrievedEvidence;
import com.raahmediq.ai.web.AiDtos.AskResponse;
import com.raahmediq.ai.web.AiDtos.AssistantStatusResponse;
import com.raahmediq.ai.web.AiDtos.CitationResponse;
import com.raahmediq.ai.web.AiDtos.ConversationResponse;
import com.raahmediq.ai.web.AiDtos.MessageResponse;
import com.raahmediq.audit.service.AuditService;
import com.raahmediq.common.error.NotFoundException;
import com.raahmediq.patient.domain.Patient;
import com.raahmediq.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AiAssistantService {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]{4,}");
    private final PatientRepository patients;
    private final AiConversationRepository conversations;
    private final AiMessageRepository messages;
    private final AiMessageCitationRepository citations;
    private final AiKnowledgeIndexService indexing;
    private final AiRetrievalService retrieval;
    private final MedicalAiSafetyPolicy safety;
    private final AiQueryRateLimiter rateLimiter;
    private final AuditService audit;
    private final Clock clock;

    public AiAssistantService(PatientRepository patients, AiConversationRepository conversations,
                              AiMessageRepository messages, AiMessageCitationRepository citations,
                              AiKnowledgeIndexService indexing, AiRetrievalService retrieval,
                              MedicalAiSafetyPolicy safety, AiQueryRateLimiter rateLimiter,
                              AuditService audit, Clock clock) {
        this.patients = patients;
        this.conversations = conversations;
        this.messages = messages;
        this.citations = citations;
        this.indexing = indexing;
        this.retrieval = retrieval;
        this.safety = safety;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public ConversationResponse createConversation(UUID userId) {
        Patient patient = patient(userId);
        AiConversation conversation = conversations.save(new AiConversation(patient, "New care conversation", clock.instant()));
        audit.record("AI_CONVERSATION_CREATED", "AI_CONVERSATION", conversation.getId(), null);
        return conversation(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations(UUID userId) {
        Patient patient = patient(userId);
        return conversations.findAllByPatientIdOrderByLastActivityAtDesc(patient.getId()).stream()
                .map(this::conversation).toList();
    }

    @Transactional
    public List<MessageResponse> messages(UUID userId, UUID conversationId) {
        Patient patient = patient(userId);
        AiConversation conversation = ownedConversation(patient, conversationId);
        List<MessageResponse> result = messages.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::message).toList();
        audit.record("AI_CONVERSATION_VIEWED", "AI_CONVERSATION", conversation.getId(), null);
        return result;
    }

    @Transactional
    public AskResponse ask(UUID userId, UUID conversationId, String rawQuestion) {
        rateLimiter.check(userId);
        Patient patient = patient(userId);
        AiConversation conversation = ownedConversation(patient, conversationId);
        String question = rawQuestion.replaceAll("[\\p{Cc}&&[^\\n\\t]]", " ").replaceAll("\\s+", " ").trim();
        conversation.titleFromQuestion(question);
        conversation.touch(clock.instant());
        messages.save(new AiMessage(conversation, AiMessageRole.USER, question, AiSafetyClass.NORMAL, false));

        MedicalAiSafetyPolicy.PolicyResult policy = safety.evaluate(question);
        if (policy.safetyClass() != AiSafetyClass.NORMAL) {
            AiMessage response = messages.save(new AiMessage(conversation, AiMessageRole.ASSISTANT,
                    policy.response(), policy.safetyClass(), false));
            audit.record("AI_ASSISTANT_QUERY_SAFETY_REDIRECT", "AI_CONVERSATION", conversation.getId(), null);
            return new AskResponse(conversation.getId(), message(response));
        }

        indexing.ensureIndexed(patient);
        List<RetrievedEvidence> evidence = selectForIntent(question, retrieval.retrieve(patient.getId(), question));
        if (evidence.isEmpty()) {
            AiMessage response = messages.save(new AiMessage(conversation, AiMessageRole.ASSISTANT,
                    unavailable(), AiSafetyClass.EVIDENCE_UNAVAILABLE, false));
            audit.record("AI_ASSISTANT_QUERY_NO_EVIDENCE", "AI_CONVERSATION", conversation.getId(), null);
            return new AskResponse(conversation.getId(), message(response));
        }

        boolean documentContentQuestion = isDocumentIntent(question) && asksForDocumentContent(question);
        boolean hasExtractedEvidence = evidence.stream().anyMatch(item -> item.chunk().isExtractedText());
        String answer;
        AiSafetyClass safetyClass;
        if (documentContentQuestion && !hasExtractedEvidence) {
            answer = "I found the report metadata in your authorized record, but its text is not available for grounded summarization. It may be a scanned image, encrypted PDF, or a PDF without extractable text. Please open the source or ask the hospital to provide an accessible text PDF. I will not guess what the report says.";
            safetyClass = AiSafetyClass.EVIDENCE_UNAVAILABLE;
        } else {
            answer = compose(question, evidence);
            safetyClass = AiSafetyClass.NORMAL;
        }
        AiMessage response = messages.save(new AiMessage(conversation, AiMessageRole.ASSISTANT,
                answer, safetyClass, true));
        int order = 1;
        for (RetrievedEvidence item : evidence) {
            citations.save(new AiMessageCitation(response, item.chunk(), order++));
        }
        audit.record("AI_ASSISTANT_GROUNDED_QUERY", "AI_CONVERSATION", conversation.getId(), null);
        return new AskResponse(conversation.getId(), message(response));
    }

    public AssistantStatusResponse status() {
        return new AssistantStatusResponse("PRIVATE_LOCAL_GROUNDED", true, true, false,
                "Text PDFs are extracted; JPG/PNG and image-only PDFs require approved OCR.");
    }

    private List<RetrievedEvidence> selectForIntent(String question, List<RetrievedEvidence> evidence) {
        KnowledgeSourceType preferred = isDocumentIntent(question) ? KnowledgeSourceType.MEDICAL_DOCUMENT
                : isClinicalIntent(question) ? KnowledgeSourceType.CLINICAL_VISIT : null;
        if (preferred == null) return evidence;
        List<RetrievedEvidence> filtered = evidence.stream()
                .filter(item -> item.chunk().getSourceType() == preferred).toList();
        return filtered.isEmpty() ? List.of() : filtered;
    }

    private String compose(String question, List<RetrievedEvidence> evidence) {
        StringBuilder answer = new StringBuilder("I found these details in your authorized RaahMediQ Health record:\n\n");
        int number = 1;
        for (RetrievedEvidence item : evidence) {
            answer.append('[').append(number++).append("] ")
                    .append(relevantExcerpt(item.chunk().getContent(), question, 460)).append("\n\n");
        }
        answer.append("Source numbers match the citations below. This is a summary of existing records, not a new diagnosis or treatment recommendation.");
        return answer.toString();
    }

    private String relevantExcerpt(String content, String question, int limit) {
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= limit) return compact;
        String lower = compact.toLowerCase(Locale.ROOT);
        int match = -1;
        var matcher = WORD.matcher(question.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            int candidate = lower.indexOf(matcher.group());
            if (candidate >= 0 && (match < 0 || candidate < match)) match = candidate;
        }
        int start = match < 0 ? 0 : Math.max(0, match - 90);
        int end = Math.min(compact.length(), start + limit);
        return (start > 0 ? "…" : "") + compact.substring(start, end).trim() + (end < compact.length() ? "…" : "");
    }

    private String unavailable() {
        return "I could not find supporting evidence for that question in your authorized RaahMediQ Health record. I will not guess or use another patient's data. Try asking about an uploaded report, prescribed medicine, diagnosis entered by your doctor, allergy, or follow-up recommendation—or contact your care team.";
    }

    private boolean isDocumentIntent(String question) {
        String value = question.toLowerCase(Locale.ROOT);
        return containsAny(value, "report", "mri", "ct scan", "x-ray", "xray", "lab", "document", "scan result");
    }

    private boolean isClinicalIntent(String question) {
        String value = question.toLowerCase(Locale.ROOT);
        return containsAny(value, "medicine", "medication", "prescription", "follow up", "follow-up", "diagnosis",
                "symptom", "allergy", "doctor note", "consultation", "discharge");
    }

    private boolean asksForDocumentContent(String question) {
        String value = question.toLowerCase(Locale.ROOT);
        return containsAny(value, "what did", "what does", "say", "finding", "result", "value", "summar", "mean", "mention");
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private Patient patient(UUID userId) {
        return patients.findByUserId(userId).orElseThrow(() -> new NotFoundException("Patient profile not found."));
    }

    private AiConversation ownedConversation(Patient patient, UUID conversationId) {
        return conversations.findByIdAndPatientId(conversationId, patient.getId())
                .orElseThrow(() -> new NotFoundException("Care-assistant conversation not found."));
    }

    private ConversationResponse conversation(AiConversation value) {
        return new ConversationResponse(value.getId(), value.getTitle(), value.getLastActivityAt());
    }

    private MessageResponse message(AiMessage value) {
        List<CitationResponse> messageCitations = value.getRole() == AiMessageRole.ASSISTANT
                ? citations.findAllByMessageIdOrderByCitationOrderAsc(value.getId()).stream()
                    .map(citation -> citation(citation.getChunk(), citation.getCitationOrder())).toList()
                : List.of();
        return new MessageResponse(value.getId(), value.getRole().name(), value.getContent(),
                value.getSafetyClass().name(), value.isGrounded(), value.getCreatedAt(), messageCitations);
    }

    private CitationResponse citation(com.raahmediq.ai.domain.DocumentChunk chunk, int number) {
        UUID sourceId = chunk.getSourceKey();
        String provenance = chunk.getSourceType() == KnowledgeSourceType.CLINICAL_VISIT
                ? "Clinician finalized"
                : chunk.getMedicalDocument().getVerificationStatus().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return new CitationResponse(chunk.getId(), number, chunk.getSourceType().name(), sourceId,
                chunk.getCitationLabel(), chunk.getPageNumber(), relevantExcerpt(chunk.getContent(), "", 260),
                provenance, "/records");
    }
}
