package com.raahmediq.ambulance.repository;

import com.raahmediq.ambulance.domain.Ambulance;
import com.raahmediq.ambulance.domain.AmbulanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbulanceRepository extends JpaRepository<Ambulance, UUID> {
    List<Ambulance> findAllByHospitalIdAndActiveTrueOrderByCallSignAsc(UUID hospitalId);
    long countByHospitalIdAndActiveTrue(UUID hospitalId);
    long countByHospitalIdAndActiveTrueAndStatus(UUID hospitalId, AmbulanceStatus status);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
    boolean existsByHospitalIdAndCallSignIgnoreCase(UUID hospitalId, String callSign);
    Optional<Ambulance> findByRegistrationNumberIgnoreCase(String registrationNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Ambulance item where item.id = :id")
    Optional<Ambulance> findByIdForUpdate(@Param("id") UUID id);
}
