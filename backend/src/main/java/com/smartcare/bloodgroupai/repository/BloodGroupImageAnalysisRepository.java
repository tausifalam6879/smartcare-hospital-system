package com.smartcare.bloodgroupai.repository;

import com.smartcare.bloodgroupai.domain.BloodGroupAnalysisStatus;
import com.smartcare.bloodgroupai.domain.BloodGroupImageAnalysis;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodGroupImageAnalysisRepository extends JpaRepository<BloodGroupImageAnalysis, UUID> {
    List<BloodGroupImageAnalysis> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<BloodGroupImageAnalysis> findAllByHospitalIdAndStatusOrderByCreatedAtAsc(
            UUID hospitalId, BloodGroupAnalysisStatus status);
    List<BloodGroupImageAnalysis> findAllByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select analysis from BloodGroupImageAnalysis analysis where analysis.id = :id")
    Optional<BloodGroupImageAnalysis> findByIdForUpdate(@Param("id") UUID id);
}
