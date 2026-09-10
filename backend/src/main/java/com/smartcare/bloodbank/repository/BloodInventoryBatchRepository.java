package com.smartcare.bloodbank.repository;

import com.smartcare.bloodbank.domain.BloodComponent;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodbank.domain.BloodInventoryBatch;
import com.smartcare.bloodbank.domain.InventoryVerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodInventoryBatchRepository extends JpaRepository<BloodInventoryBatch, UUID> {

    List<BloodInventoryBatch> findAllByBloodBankHospitalId(UUID hospitalId);

    List<BloodInventoryBatch> findAllByBloodBankHospitalIdAndBloodGroupAndComponent(
            UUID hospitalId, BloodGroup bloodGroup, BloodComponent component);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from BloodInventoryBatch item where item.bloodBank.id = :bankId "
            + "and lower(item.batchReference) = lower(:batchReference)")
    Optional<BloodInventoryBatch> findByBankAndBatchForUpdate(@Param("bankId") UUID bankId,
                                                              @Param("batchReference") String batchReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from BloodInventoryBatch item where item.id = :id")
    Optional<BloodInventoryBatch> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from BloodInventoryBatch item join fetch item.bloodBank bank "
            + "where bank.hospital.id = :hospitalId and bank.authorized = true and bank.active = true "
            + "and item.bloodGroup = :bloodGroup and item.component = :component "
            + "and item.verificationStatus = :status and item.lastVerifiedAt >= :verifiedAfter "
            + "and item.expiresOn >= :today and item.totalUnits > item.reservedUnits "
            + "order by bank.distanceKm asc, item.expiresOn asc, item.lastVerifiedAt desc")
    List<BloodInventoryBatch> findEligibleForUpdate(@Param("hospitalId") UUID hospitalId,
                                                     @Param("bloodGroup") BloodGroup bloodGroup,
                                                     @Param("component") BloodComponent component,
                                                     @Param("status") InventoryVerificationStatus status,
                                                     @Param("verifiedAfter") Instant verifiedAfter,
                                                     @Param("today") LocalDate today);
}
