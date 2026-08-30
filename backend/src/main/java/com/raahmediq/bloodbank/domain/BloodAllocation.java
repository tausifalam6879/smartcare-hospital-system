package com.raahmediq.bloodbank.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "blood_allocations", uniqueConstraints = @UniqueConstraint(
        name = "uk_blood_allocation_request_batch", columnNames = {"blood_request_id", "inventory_batch_id"}))
public class BloodAllocation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_batch_id", nullable = false)
    private BloodInventoryBatch inventoryBatch;

    @Column(nullable = false)
    private int units;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BloodAllocationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected BloodAllocation() {
    }

    public BloodAllocation(BloodRequest request, BloodInventoryBatch inventoryBatch, int units, Instant reservedAt) {
        this.request = request;
        this.inventoryBatch = inventoryBatch;
        this.units = units;
        this.status = BloodAllocationStatus.RESERVED;
        this.reservedAt = reservedAt;
    }

    public void addUnits(int extra) {
        if (status != BloodAllocationStatus.RESERVED || extra <= 0) {
            throw new IllegalStateException("Only an active allocation can receive more units.");
        }
        units += extra;
    }

    public void fulfil(Instant at) {
        if (status != BloodAllocationStatus.RESERVED) throw new IllegalStateException("Allocation is not active.");
        status = BloodAllocationStatus.FULFILLED;
        resolvedAt = at;
    }

    public void release(Instant at) {
        if (status != BloodAllocationStatus.RESERVED) throw new IllegalStateException("Allocation is not active.");
        status = BloodAllocationStatus.RELEASED;
        resolvedAt = at;
    }

    public BloodRequest getRequest() { return request; }
    public BloodInventoryBatch getInventoryBatch() { return inventoryBatch; }
    public int getUnits() { return units; }
    public BloodAllocationStatus getStatus() { return status; }
    public Instant getReservedAt() { return reservedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
