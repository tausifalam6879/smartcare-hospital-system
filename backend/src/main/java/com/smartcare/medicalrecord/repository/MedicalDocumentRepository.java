package com.smartcare.medicalrecord.repository;

import com.smartcare.medicalrecord.domain.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {
    List<MedicalDocument> findAllByPatientIdOrderByDocumentDateDescCreatedAtDesc(UUID patientId);
    Optional<MedicalDocument> findByIdAndPatientId(UUID id, UUID patientId);
}
