package com.smartcare.followup.repository;

import com.smartcare.followup.domain.CareFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareFollowUpRepository extends JpaRepository<CareFollowUp, UUID> {
    List<CareFollowUp> findAllByPatientIdOrderByFollowUpDateAsc(UUID patientId);
    Optional<CareFollowUp> findByIdAndPatientId(UUID id, UUID patientId);
}
