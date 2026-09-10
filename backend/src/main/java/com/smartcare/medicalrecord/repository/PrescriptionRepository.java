package com.smartcare.medicalrecord.repository;

import com.smartcare.medicalrecord.domain.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    Optional<Prescription> findByClinicalVisitId(UUID clinicalVisitId);
}
