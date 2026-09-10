package com.smartcare.navigation.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.hospital.domain.Hospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "navigation_paths", uniqueConstraints =
        @UniqueConstraint(name = "uk_navigation_path_nodes", columnNames = {"hospital_id", "from_location_id", "to_location_id"}))
public class NavigationPath extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_location_id", nullable = false)
    private HospitalLocation fromLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_location_id", nullable = false)
    private HospitalLocation toLocation;

    @Column(name = "instruction_en", nullable = false, length = 400)
    private String instructionEn;

    @Column(name = "instruction_hi", nullable = false, length = 500)
    private String instructionHi;

    @Column(name = "reverse_instruction_en", nullable = false, length = 400)
    private String reverseInstructionEn;

    @Column(name = "reverse_instruction_hi", nullable = false, length = 500)
    private String reverseInstructionHi;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "step_free", nullable = false)
    private boolean stepFree;

    @Column(nullable = false)
    private boolean active;

    protected NavigationPath() {
    }

    public NavigationPath(Hospital hospital, HospitalLocation fromLocation, HospitalLocation toLocation,
                          String instructionEn, String instructionHi, String reverseInstructionEn,
                          String reverseInstructionHi, int distanceMeters, int durationSeconds, boolean stepFree) {
        this.hospital = hospital;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.instructionEn = instructionEn;
        this.instructionHi = instructionHi;
        this.reverseInstructionEn = reverseInstructionEn;
        this.reverseInstructionHi = reverseInstructionHi;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.stepFree = stepFree;
        this.active = true;
    }

    public Hospital getHospital() { return hospital; }
    public HospitalLocation getFromLocation() { return fromLocation; }
    public HospitalLocation getToLocation() { return toLocation; }
    public String getInstructionEn() { return instructionEn; }
    public String getInstructionHi() { return instructionHi; }
    public String getReverseInstructionEn() { return reverseInstructionEn; }
    public String getReverseInstructionHi() { return reverseInstructionHi; }
    public int getDistanceMeters() { return distanceMeters; }
    public int getDurationSeconds() { return durationSeconds; }
    public boolean isStepFree() { return stepFree; }
    public boolean isActive() { return active; }
}
