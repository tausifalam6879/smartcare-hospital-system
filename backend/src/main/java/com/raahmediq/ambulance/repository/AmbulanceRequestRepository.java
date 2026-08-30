package com.raahmediq.ambulance.repository;

import com.raahmediq.ambulance.domain.AmbulanceRequest;
import com.raahmediq.ambulance.domain.AmbulanceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbulanceRequestRepository extends JpaRepository<AmbulanceRequest, UUID> {
    Optional<AmbulanceRequest> findByRequestedByIdAndIdempotencyKey(UUID requestedById, String idempotencyKey);
    List<AmbulanceRequest> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<AmbulanceRequest> findAllByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);
    List<AmbulanceRequest> findAllByHospitalIdAndStatusOrderByCreatedAtDesc(UUID hospitalId,
                                                                            AmbulanceRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from AmbulanceRequest item where item.id = :id")
    Optional<AmbulanceRequest> findByIdForUpdate(@Param("id") UUID id);
}
