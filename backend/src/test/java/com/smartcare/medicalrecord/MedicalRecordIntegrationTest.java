package com.smartcare.medicalrecord;

import com.smartcare.appointment.domain.PaymentMethod;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.appointment.web.AppointmentDtos.BookingRequest;
import com.smartcare.audit.repository.AuditLogRepository;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.AuthResponse;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.checkin.domain.CheckInChannel;
import com.smartcare.checkin.service.CheckInService;
import com.smartcare.common.error.ConflictException;
import com.smartcare.doctor.service.DoctorService;
import com.smartcare.doctor.web.DoctorDtos.DoctorRequest;
import com.smartcare.doctor.web.DoctorDtos.LinkAccountRequest;
import com.smartcare.doctor.web.DoctorDtos.ScheduleRequest;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.followup.domain.FollowUpStatus;
import com.smartcare.followup.service.CareFollowUpService;
import com.smartcare.medicalrecord.domain.AllergySeverity;
import com.smartcare.medicalrecord.domain.DocumentType;
import com.smartcare.medicalrecord.service.MedicalRecordService;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.AllergyRequest;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.PrescriptionItemRequest;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.VisitRecordRequest;
import com.smartcare.payment.service.PaymentService;
import com.smartcare.queue.service.QueueService;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MedicalRecordIntegrationTest {

    @Autowired MedicalRecordService records;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;
    @Autowired AppointmentService appointments;
    @Autowired PaymentService payments;
    @Autowired CheckInService checkIns;
    @Autowired QueueService queues;
    @Autowired AuthService auth;
    @Autowired AuditLogRepository auditLogs;
    @Autowired CareFollowUpService followUps;
    @Autowired NotificationService notifications;

    @Test
    @WithMockUser(roles = {"HOSPITAL_ADMIN", "CASHIER", "RECEPTIONIST"})
    void finalizedVisitAndPrivateDocumentRemainPatientIsolatedAndAudited() {
        LocalDate today = LocalDate.now();
        var hospital = hospitals.create(new HospitalRequest("SC-RECORD-1", "Record Test Hospital",
                "9 Privacy Road", "Delhi", "Delhi", "110001", "+911112345699", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("MED", "Medicine", "Clinical record test"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Record Test",
                "Internal Medicine", "SC-RECORD-REG-1", new BigDecimal("650.00"), 15, 10,
                "Care Block", "1st Floor", "OPD 10", true));
        doctors.addSchedule(doctor.id(), new ScheduleRequest(today.getDayOfWeek(),
                LocalTime.of(0, 1), LocalTime.of(23, 59), 15, 10));

        AuthResponse patient = patient("41", "record.owner");
        AuthResponse anotherPatient = patient("42", "record.other");
        AuthResponse doctorAccount = patient("43", "record.doctor");
        doctors.linkAccount(doctor.id(), new LinkAccountRequest(doctorAccount.user().id()));

        var appointment = appointments.book(patient.user().id(), "record-appointment-1",
                new BookingRequest(doctor.id(), today, PaymentMethod.CASH));
        payments.confirmCash(appointment.id());
        checkIns.checkIn(patient.user().id(), List.of("PATIENT"), appointment.id(), CheckInChannel.MOBILE_WEB);
        queues.serveNext(doctor.id(), today);

        var visit = records.recordVisit(doctorAccount.user().id(), new VisitRecordRequest(appointment.id(),
                "Fever and sore throat for two days", "Viral upper respiratory infection",
                "Hydration discussed; return if symptoms worsen.", null,
                "Follow up in five days if fever continues.", "Take after food.",
                List.of(new PrescriptionItemRequest("Paracetamol", "500 mg", "Twice daily", "3 days",
                        "Oral", "Only as advised.")),
                List.of(new AllergyRequest("Penicillin", "Skin rash", AllergySeverity.HIGH)),
                today.plusDays(5), true));
        assertThat(visit.diagnosis()).contains("Viral");
        assertThat(visit.prescription().medicines()).extracting(item -> item.medicineName())
                .containsExactly("Paracetamol");
        assertThat(appointments.mine(patient.user().id())).singleElement()
                .satisfies(item -> assertThat(item.status()).isEqualTo(AppointmentStatus.COMPLETED));
        assertThat(notifications.mine(patient.user().id())).extracting(item -> item.type())
                .contains(NotificationType.VISIT_COMPLETED);
        var followUp = followUps.mine(patient.user().id()).get(0);
        assertThat(followUp.followUpDate()).isEqualTo(today.plusDays(5));
        assertThat(followUp.medicationReminderEnabled()).isTrue();
        assertThat(followUps.updateStatus(patient.user().id(), followUp.id(), FollowUpStatus.CONFIRMED).status())
                .isEqualTo(FollowUpStatus.CONFIRMED);
        assertThatThrownBy(() -> followUps.updateStatus(patient.user().id(), followUp.id(), FollowUpStatus.COMPLETED))
                .isInstanceOf(ConflictException.class).hasMessageContaining("scheduled date");
        assertThatThrownBy(() -> followUps.updateStatus(anotherPatient.user().id(), followUp.id(),
                FollowUpStatus.COMPLETED)).isInstanceOf(com.smartcare.common.error.NotFoundException.class);

        byte[] pdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        var uploaded = records.upload(patient.user().id(), hospital.id(), DocumentType.LAB_REPORT, today,
                "Outside CBC report", new MockMultipartFile("file", "cbc-report.pdf", "application/pdf", pdf));
        assertThat(uploaded.contentPath()).doesNotContain("private-documents");
        assertThat(records.download(patient.user().id(), uploaded.id()).content()).isEqualTo(pdf);

        var ownRecord = records.mine(patient.user().id());
        assertThat(ownRecord.visits()).hasSize(1);
        assertThat(ownRecord.allergies()).extracting(item -> item.substance()).containsExactly("Penicillin");
        assertThat(ownRecord.documents()).extracting(item -> item.originalFilename()).containsExactly("cbc-report.pdf");
        assertThat(records.forAppointment(doctorAccount.user().id(), appointment.id()).patientNumber())
                .isEqualTo(ownRecord.patientNumber());

        assertThatThrownBy(() -> records.download(anotherPatient.user().id(), uploaded.id()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> records.upload(patient.user().id(), hospital.id(), DocumentType.OTHER, today,
                null, new MockMultipartFile("file", "fake.pdf", "application/pdf", "not-pdf".getBytes())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("genuine PDF");
        assertThat(auditLogs.count()).isGreaterThanOrEqualTo(8);
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9192000000" + digits, label + "@example.com",
                "Record User", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
