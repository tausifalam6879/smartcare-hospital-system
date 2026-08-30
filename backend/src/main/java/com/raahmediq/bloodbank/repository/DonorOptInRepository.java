package com.raahmediq.bloodbank.repository;

import com.raahmediq.bloodbank.domain.BloodGroup;
import com.raahmediq.bloodbank.domain.DonorEligibilityStatus;
import com.raahmediq.bloodbank.domain.DonorOptIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonorOptInRepository extends JpaRepository<DonorOptIn, UUID> {
    Optional<DonorOptIn> findByUserId(UUID userId);
    List<DonorOptIn> findAllByVerifiedBloodGroupAndEligibilityStatus(
            BloodGroup bloodGroup, DonorEligibilityStatus eligibilityStatus);
}
