package com.raahmediq.ai;

import com.raahmediq.ai.service.AiAssistantService;
import com.raahmediq.audit.repository.AuditLogRepository;
import com.raahmediq.auth.service.AuthService;
import com.raahmediq.auth.web.AuthResponse;
import com.raahmediq.auth.web.RegisterRequest;
import com.raahmediq.common.error.NotFoundException;
import com.raahmediq.hospital.service.HospitalService;
import com.raahmediq.hospital.web.HospitalDtos.HospitalRequest;
import com.raahmediq.medicalrecord.domain.DocumentType;
import com.raahmediq.medicalrecord.service.MedicalRecordService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AiAssistantIntegrationTest {

    @Autowired AiAssistantService assistant;
    @Autowired MedicalRecordService records;
    @Autowired HospitalService hospitals;
    @Autowired AuthService auth;
    @Autowired AuditLogRepository auditLogs;

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void retrievalIsPatientIsolatedCitedAndSafetyBounded() throws Exception {
        var hospital = hospitals.create(new HospitalRequest("RMQ-AI-1", "Grounded Care Hospital",
                "7 Evidence Road", "Delhi", "Delhi", "110001", "+911112345688", "Asia/Kolkata", true));
        AuthResponse owner = patient("51", "rag.owner");
        AuthResponse other = patient("52", "rag.other");

        byte[] pdf = textPdf("CBC result: Haemoglobin 13.4 g/dL. Platelet count 240000 per microlitre.");
        records.upload(owner.user().id(), hospital.id(), DocumentType.LAB_REPORT, LocalDate.now(),
                "CBC from follow-up", new MockMultipartFile("file", "cbc-grounded.pdf", "application/pdf", pdf));

        var ownerConversation = assistant.createConversation(owner.user().id());
        var grounded = assistant.ask(owner.user().id(), ownerConversation.id(),
                "What did my CBC report say about haemoglobin?");
        assertThat(grounded.message().grounded()).isTrue();
        assertThat(grounded.message().content()).contains("Haemoglobin 13.4 g/dL");
        assertThat(grounded.message().citations()).hasSize(1);
        assertThat(grounded.message().citations().get(0).sourceType()).isEqualTo("MEDICAL_DOCUMENT");
        assertThat(grounded.message().citations().get(0).pageNumber()).isEqualTo(1);

        var otherConversation = assistant.createConversation(other.user().id());
        var isolated = assistant.ask(other.user().id(), otherConversation.id(),
                "What did my CBC report say about haemoglobin?");
        assertThat(isolated.message().safetyClass()).isEqualTo("EVIDENCE_UNAVAILABLE");
        assertThat(isolated.message().content()).doesNotContain("13.4", "240000");
        assertThat(isolated.message().citations()).isEmpty();
        assertThatThrownBy(() -> assistant.messages(other.user().id(), ownerConversation.id()))
                .isInstanceOf(NotFoundException.class);

        var emergency = assistant.ask(owner.user().id(), ownerConversation.id(),
                "I have severe chest pain and difficulty breathing");
        assertThat(emergency.message().safetyClass()).isEqualTo("EMERGENCY_ESCALATION");
        assertThat(emergency.message().content()).contains("emergency");
        assertThat(emergency.message().citations()).isEmpty();

        var boundary = assistant.ask(owner.user().id(), ownerConversation.id(),
                "What should I take and can you change my dose?");
        assertThat(boundary.message().safetyClass()).isEqualTo("CLINICAL_BOUNDARY");
        assertThat(boundary.message().content()).contains("cannot diagnose, prescribe, change a dose");
        assertThat(auditLogs.count()).isGreaterThanOrEqualTo(7);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9193000000" + digits, label + "@example.com",
                "RAG Test User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }

    private byte[] textPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 720);
                stream.showText(text);
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
