package com.smartcare.followup.service;

import com.smartcare.audit.service.AuditService;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.followup.domain.CareFollowUp;
import com.smartcare.followup.domain.FollowUpStatus;
import com.smartcare.followup.repository.CareFollowUpRepository;
import com.smartcare.followup.web.FollowUpDtos.FollowUpResponse;
import com.smartcare.medicalrecord.domain.ClinicalVisit;
import com.smartcare.notification.domain.NotificationType;
import com.smartcare.notification.service.NotificationService;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CareFollowUpService {
    private final CareFollowUpRepository followUps;
    private final PatientRepository patients;
    private final NotificationService notifications;
    private final AuditService audit;
    private final Clock clock;

    public CareFollowUpService(CareFollowUpRepository followUps, PatientRepository patients,
                               NotificationService notifications, AuditService audit, Clock clock) {
        this.followUps = followUps;
        this.patients = patients;
        this.notifications = notifications;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public CareFollowUp schedule(ClinicalVisit visit, LocalDate date, boolean medicationReminderEnabled) {
        if (date.isBefore(visit.getVisitDate())) {
            throw new IllegalArgumentException("Follow-up date cannot be before the visit date.");
        }
        CareFollowUp followUp = followUps.save(new CareFollowUp(visit, date,
                visit.getFollowUpRecommendation(), medicationReminderEnabled));
        notifications.notifyPatient(visit.getPatient(), visit.getHospital().getId(),
                NotificationType.FOLLOW_UP_SCHEDULED, followUp.getId(), "scheduled",
                "Follow-up scheduled", "Your follow-up with " + visit.getDoctor().getName() + " is scheduled for " + date + ".");
        if (medicationReminderEnabled) {
            notifications.notifyPatient(visit.getPatient(), visit.getHospital().getId(),
                    NotificationType.MEDICATION_REMINDER_CREATED, followUp.getId(), "medication-plan",
                    "Medication reminder active", "Follow the exact medicine schedule in your doctor-issued prescription. Do not change a dose without clinical advice.");
        }
        audit.record("FOLLOW_UP_SCHEDULED", "CARE_FOLLOW_UP", followUp.getId(), visit.getHospital().getId());
        return followUp;
    }

    @Transactional(readOnly = true)
    public List<FollowUpResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        return followUps.findAllByPatientIdOrderByFollowUpDateAsc(patient.getId()).stream().map(this::response).toList();
    }

    @Transactional
    public FollowUpResponse updateStatus(UUID userId, UUID followUpId, FollowUpStatus status) {
        Patient patient = requirePatient(userId);
        CareFollowUp followUp = followUps.findByIdAndPatientId(followUpId, patient.getId())
                .orElseThrow(() -> new NotFoundException("Follow-up was not found."));
        followUp.updateStatus(status, clock.instant());
        audit.record("FOLLOW_UP_STATUS_UPDATED", "CARE_FOLLOW_UP", followUp.getId(), followUp.getHospital().getId());
        return response(followUp);
    }

    private FollowUpResponse response(CareFollowUp item) {
        boolean overdue = item.getFollowUpDate().isBefore(LocalDate.now(clock))
                && item.getStatus() != FollowUpStatus.COMPLETED;
        return new FollowUpResponse(item.getId(), item.getClinicalVisit().getId(),
                item.getClinicalVisit().getVisitDate(), item.getFollowUpDate(), item.getHospital().getName(),
                item.getDoctor().getName(), item.getDoctor().getSpecialization(), item.getInstructions(),
                item.isMedicationReminderEnabled(), item.getStatus(), overdue, item.getPatientResponseAt());
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }
}
