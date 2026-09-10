package com.smartcare.bloodbank.repository;

import com.smartcare.bloodbank.domain.BloodRequest;
import com.smartcare.bloodbank.domain.BloodRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, UUID> {
    Optional<BloodRequest> findByCreatedByIdAndIdempotencyKey(UUID createdById, String idempotencyKey);
    List<BloodRequest> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<BloodRequest> findAllByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);
    List<BloodRequest> findAllByHospitalIdAndStatusOrderByCreatedAtDesc(UUID hospitalId, BloodRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from BloodRequest request where request.id = :id")
    Optional<BloodRequest> findByIdForUpdate(@Param("id") UUID id);
}
