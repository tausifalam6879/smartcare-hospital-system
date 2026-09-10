package com.smartcare.bloodbank.repository;

import com.smartcare.bloodbank.domain.BloodBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodBankRepository extends JpaRepository<BloodBank, UUID> {
    List<BloodBank> findAllByHospitalIdAndAuthorizedTrueAndActiveTrueOrderByDistanceKmAscNameAsc(UUID hospitalId);
    Optional<BloodBank> findByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);
    boolean existsByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);
}
