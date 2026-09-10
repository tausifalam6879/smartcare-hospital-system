package com.smartcare.medicalrecord.repository;

import com.smartcare.medicalrecord.domain.ClinicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalVisitRepository extends JpaRepository<ClinicalVisit, UUID> {
    List<ClinicalVisit> findAllByPatientIdOrderByVisitDateDescCreatedAtDesc(UUID patientId);
    Optional<ClinicalVisit> findByAppointmentId(UUID appointmentId);
}
