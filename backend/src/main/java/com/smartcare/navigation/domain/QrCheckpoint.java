package com.smartcare.navigation.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.hospital.domain.Hospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "qr_checkpoints")
public class QrCheckpoint extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, unique = true)
    private HospitalLocation location;

    @Column(name = "public_code", nullable = false, unique = true, length = 64)
    private String publicCode;

    @Column(name = "label_en", nullable = false, length = 160)
    private String labelEn;

    @Column(name = "label_hi", nullable = false, length = 200)
    private String labelHi;

    @Column(nullable = false)
    private boolean active;

    protected QrCheckpoint() {
    }

    public QrCheckpoint(Hospital hospital, HospitalLocation location, String publicCode,
                        String labelEn, String labelHi) {
        this.hospital = hospital;
        this.location = location;
        this.publicCode = publicCode;
        this.labelEn = labelEn;
        this.labelHi = labelHi;
        this.active = true;
    }

    public Hospital getHospital() { return hospital; }
    public HospitalLocation getLocation() { return location; }
    public String getPublicCode() { return publicCode; }
    public String getLabelEn() { return labelEn; }
    public String getLabelHi() { return labelHi; }
    public boolean isActive() { return active; }
}
