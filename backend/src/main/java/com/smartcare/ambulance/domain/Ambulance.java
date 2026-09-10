package com.smartcare.ambulance.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.hospital.domain.Hospital;
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
import java.time.Instant;

@Entity
@Table(name = "ambulances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ambulance_registration", columnNames = "registration_number"),
        @UniqueConstraint(name = "uk_ambulance_call_sign", columnNames = {"hospital_id", "call_sign"})
})
public class Ambulance extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "registration_number", nullable = false, length = 30)
    private String registrationNumber;

    @Column(name = "call_sign", nullable = false, length = 40)
    private String callSign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AmbulanceStatus status;

    @Column(name = "crew_label", nullable = false, length = 120)
    private String crewLabel;

    @Column(name = "crew_contact", length = 20)
    private String crewContact;

    @Column(name = "current_area", length = 180)
    private String currentArea;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean synthetic;

    protected Ambulance() {
    }

    public Ambulance(Hospital hospital, String registrationNumber, String callSign, String crewLabel,
                     String crewContact, String currentArea, boolean synthetic) {
        this.hospital = hospital;
        this.registrationNumber = registrationNumber;
        this.callSign = callSign;
        this.crewLabel = crewLabel;
        this.crewContact = crewContact;
        this.currentArea = currentArea;
        this.status = AmbulanceStatus.AVAILABLE;
        this.active = true;
        this.synthetic = synthetic;
    }

    public void assign() {
        requireStatus(AmbulanceStatus.AVAILABLE);
        status = AmbulanceStatus.ASSIGNED;
    }

    public void operationalStatus(AmbulanceStatus next) {
        if (next == AmbulanceStatus.AVAILABLE || next == AmbulanceStatus.OUT_OF_SERVICE) {
            throw new IllegalStateException("Use the explicit availability operation for this status.");
        }
        status = next;
    }

    public void release() {
        status = AmbulanceStatus.AVAILABLE;
    }

    public void markOutOfService() {
        requireStatus(AmbulanceStatus.AVAILABLE);
        status = AmbulanceStatus.OUT_OF_SERVICE;
    }

    public void returnToService() {
        requireStatus(AmbulanceStatus.OUT_OF_SERVICE);
        status = AmbulanceStatus.AVAILABLE;
    }

    public void updateLocation(String area, BigDecimal latitude, BigDecimal longitude, Instant at) {
        currentArea = area;
        this.latitude = latitude;
        this.longitude = longitude;
        locationUpdatedAt = at;
    }

    private void requireStatus(AmbulanceStatus expected) {
        if (status != expected) throw new IllegalStateException("Ambulance is not " + expected + ".");
    }

    public Hospital getHospital() { return hospital; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getCallSign() { return callSign; }
    public AmbulanceStatus getStatus() { return status; }
    public String getCrewLabel() { return crewLabel; }
    public String getCrewContact() { return crewContact; }
    public String getCurrentArea() { return currentArea; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public boolean isActive() { return active; }
    public boolean isSynthetic() { return synthetic; }
}
