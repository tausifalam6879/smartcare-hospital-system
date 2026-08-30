package com.raahmediq.bloodbank.repository;

import com.raahmediq.bloodbank.domain.BloodAllocation;
import com.raahmediq.bloodbank.domain.BloodAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodAllocationRepository extends JpaRepository<BloodAllocation, UUID> {
    List<BloodAllocation> findAllByRequestIdOrderByCreatedAtAsc(UUID requestId);
    List<BloodAllocation> findAllByRequestIdAndStatus(UUID requestId, BloodAllocationStatus status);
    Optional<BloodAllocation> findByRequestIdAndInventoryBatchId(UUID requestId, UUID inventoryBatchId);
}
