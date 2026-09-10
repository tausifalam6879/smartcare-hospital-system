package com.smartcare.bloodbank.repository;

import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodbank.domain.DonorEligibilityStatus;
import com.smartcare.bloodbank.domain.DonorOptIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonorOptInRepository extends JpaRepository<DonorOptIn, UUID> {
    Optional<DonorOptIn> findByUserId(UUID userId);
    List<DonorOptIn> findAllByVerifiedBloodGroupAndEligibilityStatus(
            BloodGroup bloodGroup, DonorEligibilityStatus eligibilityStatus);
}
