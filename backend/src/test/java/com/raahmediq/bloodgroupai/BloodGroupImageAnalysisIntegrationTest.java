package com.raahmediq.bloodgroupai;

import com.raahmediq.audit.repository.AuditLogRepository;
import com.raahmediq.auth.domain.Role;
import com.raahmediq.auth.repository.UserAccountRepository;
import com.raahmediq.auth.service.AuthService;
import com.raahmediq.auth.web.AuthResponse;
import com.raahmediq.auth.web.RegisterRequest;
import com.raahmediq.bloodbank.domain.BloodGroup;
import com.raahmediq.bloodgroupai.domain.BloodGroupAnalysisStatus;
import com.raahmediq.bloodgroupai.domain.ModelInferenceStatus;
import com.raahmediq.bloodgroupai.service.BloodGroupImageAnalysisService;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.RecordObservations;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.VerifyAnalysis;
import com.raahmediq.common.error.ConflictException;
import com.raahmediq.hospital.service.HospitalService;
import com.raahmediq.hospital.web.HospitalDtos.HospitalRequest;
import com.raahmediq.notification.domain.NotificationType;
import com.raahmediq.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BloodGroupImageAnalysisIntegrationTest {

    @Autowired BloodGroupImageAnalysisService analyses;
    @Autowired HospitalService hospitals;
    @Autowired AuthService auth;
    @Autowired UserAccountRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository auditLogs;

    @Test
    void imageNeverBecomesAResultWithoutObservedReactionsAndIndependentVerification() throws Exception {
        var hospital = hospitals.create(new HospitalRequest("RMQ-BGAI-1", "Blood Image Review Hospital",
                "12 Verification Road", "Delhi", "Delhi", "110001", "+911140404122", "Asia/Kolkata", true));
        AuthResponse patient = account("91", "blood.image.patient");
        AuthResponse other = account("92", "blood.image.other");
        AuthResponse observer = account("93", "blood.image.observer");
        AuthResponse verifier = account("94", "blood.image.verifier");
        users.findById(observer.user().id()).orElseThrow().grantRole(Role.LAB_TECHNICIAN);
        users.findById(verifier.user().id()).orElseThrow().grantRole(Role.BLOOD_BANK_STAFF);

        var image = new MockMultipartFile("file", "slide.png", "image/png", png());
        assertThatThrownBy(() -> analyses.submit(patient.user().id(), hospital.id(), false, image))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> analyses.submit(patient.user().id(), hospital.id(), true,
                new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);

        var submitted = analyses.submit(patient.user().id(), hospital.id(), true, image);
        assertThat(submitted.status()).isEqualTo(BloodGroupAnalysisStatus.SUBMITTED);
        assertThat(submitted.modelInferenceStatus()).isEqualTo(ModelInferenceStatus.NOT_CONFIGURED);
        assertThat(submitted.modelSuggestedGroup()).isNull();
        assertThat(submitted.verifiedGroup()).isNull();
        assertThat(analyses.download(patient.user().id(), submitted.id()).content()).isEqualTo(image.getBytes());
        assertThatThrownBy(() -> analyses.download(other.user().id(), submitted.id()))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(analyses.worklist(observer.user().id(), hospital.id(), BloodGroupAnalysisStatus.SUBMITTED))
                .extracting(item -> item.id()).containsExactly(submitted.id());
        var observed = analyses.recordObservations(observer.user().id(), submitted.id(),
                new RecordObservations(true, false, true, "Visible Anti-A and Anti-D agglutination."));
        assertThat(observed.status()).isEqualTo(BloodGroupAnalysisStatus.OBSERVATIONS_RECORDED);
        assertThat(observed.preliminaryGroup()).isEqualTo(BloodGroup.A_POSITIVE);
        assertThat(observed.verifiedGroup()).isNull();

        assertThatThrownBy(() -> analyses.verify(observer.user().id(), submitted.id(),
                new VerifyAnalysis(BloodGroup.A_POSITIVE))).isInstanceOf(ConflictException.class);
        var verified = analyses.verify(verifier.user().id(), submitted.id(),
                new VerifyAnalysis(BloodGroup.A_POSITIVE));
        assertThat(verified.status()).isEqualTo(BloodGroupAnalysisStatus.LAB_VERIFIED);
        assertThat(verified.verifiedGroup()).isEqualTo(BloodGroup.A_POSITIVE);
        assertThat(notifications.findAll()).extracting(item -> item.getType()).contains(
                NotificationType.BLOOD_GROUP_ANALYSIS_SUBMITTED,
                NotificationType.BLOOD_GROUP_ANALYSIS_UPDATED);
        assertThat(auditLogs.count()).isGreaterThanOrEqualTo(5);
    }

    private AuthResponse account(String digits, String label) {
        return auth.register(new RegisterRequest("+9195000000" + digits, label + "@example.com",
                "Blood Image Workflow User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRect(0, 0, 64, 64);
        graphics.setColor(new Color(140, 10, 10));
        graphics.fillOval(8, 8, 48, 48);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
