package com.smartcare.bloodbank.domain;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.common.domain.AuditableEntity;
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
import java.time.LocalDate;

@Entity
@Table(name = "blood_inventory_batches", uniqueConstraints = @UniqueConstraint(
        name = "uk_blood_inventory_batch", columnNames = {"blood_bank_id", "batch_reference"}))
public class BloodInventoryBatch extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    private BloodBank bloodBank;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 20)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BloodComponent component;

    @Column(name = "batch_reference", nullable = false, length = 80)
    private String batchReference;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Column(name = "total_units", nullable = false)
    private int totalUnits;

    @Column(name = "reserved_units", nullable = false)
    private int reservedUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private InventoryVerificationStatus verificationStatus;

    @Column(name = "last_verified_at", nullable = false)
    private Instant lastVerifiedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verified_by_user_id", nullable = false)
    private UserAccount verifiedBy;

    protected BloodInventoryBatch() {
    }

    public BloodInventoryBatch(BloodBank bloodBank, BloodGroup bloodGroup, BloodComponent component,
                               String batchReference, LocalDate expiresOn, int totalUnits,
                               InventoryVerificationStatus verificationStatus, Instant verifiedAt,
                               UserAccount verifiedBy) {
        this.bloodBank = bloodBank;
        this.bloodGroup = bloodGroup;
        this.component = component;
        this.batchReference = batchReference;
        this.expiresOn = expiresOn;
        this.totalUnits = totalUnits;
        this.reservedUnits = 0;
        this.verificationStatus = verificationStatus;
        this.lastVerifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
    }

    public void verify(LocalDate expiry, int units, InventoryVerificationStatus status,
                       Instant verifiedAt, UserAccount verifier) {
        if (units < reservedUnits) {
            throw new IllegalStateException("Total units cannot be lower than units already reserved.");
        }
        this.expiresOn = expiry;
        this.totalUnits = units;
        this.verificationStatus = status;
        this.lastVerifiedAt = verifiedAt;
        this.verifiedBy = verifier;
    }

    public int availableUnits() {
        return Math.max(0, totalUnits - reservedUnits);
    }

    public int reserve(int requested) {
        if (verificationStatus != InventoryVerificationStatus.VERIFIED || requested <= 0) return 0;
        int allocated = Math.min(requested, availableUnits());
        reservedUnits += allocated;
        return allocated;
    }

    public void release(int units) {
        if (units <= 0 || units > reservedUnits) throw new IllegalStateException("Invalid blood-unit release.");
        reservedUnits -= units;
    }

    public void fulfil(int units) {
        if (units <= 0 || units > reservedUnits || units > totalUnits) {
            throw new IllegalStateException("Invalid blood-unit fulfilment.");
        }
        reservedUnits -= units;
        totalUnits -= units;
    }

    public BloodBank getBloodBank() { return bloodBank; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public BloodComponent getComponent() { return component; }
    public String getBatchReference() { return batchReference; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public int getTotalUnits() { return totalUnits; }
    public int getReservedUnits() { return reservedUnits; }
    public InventoryVerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public UserAccount getVerifiedBy() { return verifiedBy; }
}
