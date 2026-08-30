package com.raahmediq.bloodbank.domain;

import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.hospital.domain.Hospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "blood_banks", uniqueConstraints = @UniqueConstraint(
        name = "uk_blood_bank_code", columnNames = {"hospital_id", "code"}))
public class BloodBank extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "address_line", nullable = false, length = 300)
    private String addressLine;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "distance_km", nullable = false, precision = 7, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "estimated_transfer_minutes", nullable = false)
    private int estimatedTransferMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private BloodBankSourceType sourceType;

    @Column(nullable = false)
    private boolean authorized;

    @Column(nullable = false)
    private boolean active;

    protected BloodBank() {
    }

    public BloodBank(Hospital hospital, String code, String name, String addressLine, String contactNumber,
                     BigDecimal distanceKm, int estimatedTransferMinutes, BloodBankSourceType sourceType) {
        this.hospital = hospital;
        this.code = code;
        this.name = name;
        this.addressLine = addressLine;
        this.contactNumber = contactNumber;
        this.distanceKm = distanceKm;
        this.estimatedTransferMinutes = estimatedTransferMinutes;
        this.sourceType = sourceType;
        this.authorized = true;
        this.active = true;
    }

    public void updateDirectory(String name, String addressLine, String contactNumber,
                                BigDecimal distanceKm, int estimatedTransferMinutes) {
        this.name = name;
        this.addressLine = addressLine;
        this.contactNumber = contactNumber;
        this.distanceKm = distanceKm;
        this.estimatedTransferMinutes = estimatedTransferMinutes;
    }

    public Hospital getHospital() { return hospital; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAddressLine() { return addressLine; }
    public String getContactNumber() { return contactNumber; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public int getEstimatedTransferMinutes() { return estimatedTransferMinutes; }
    public BloodBankSourceType getSourceType() { return sourceType; }
    public boolean isAuthorized() { return authorized; }
    public boolean isActive() { return active; }
}
