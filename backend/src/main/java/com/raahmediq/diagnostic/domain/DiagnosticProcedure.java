package com.raahmediq.diagnostic.domain;

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
@Table(name = "diagnostic_procedures", uniqueConstraints = @UniqueConstraint(
        name = "uk_diagnostic_procedure_code", columnNames = {"hospital_id", "code"}))
public class DiagnosticProcedure extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiagnosticModality modality;

    @Column(name = "preparation_instructions", length = 1200)
    private String preparationInstructions;

    @Column(name = "turnaround_hours", nullable = false)
    private int turnaroundHours;

    @Column(name = "daily_capacity", nullable = false)
    private int dailyCapacity;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private int estimatedDurationMinutes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;

    @Column(length = 80)
    private String building;

    @Column(name = "floor_label", length = 40)
    private String floorLabel;

    @Column(name = "room_number", length = 40)
    private String roomNumber;

    @Column(nullable = false)
    private boolean active;

    protected DiagnosticProcedure() {
    }

    public DiagnosticProcedure(Hospital hospital, String code, String name, DiagnosticModality modality,
                               String preparationInstructions, int turnaroundHours, int dailyCapacity,
                               int estimatedDurationMinutes, BigDecimal fee, String building,
                               String floorLabel, String roomNumber) {
        this.hospital = hospital;
        this.code = code;
        this.name = name;
        this.modality = modality;
        this.preparationInstructions = preparationInstructions;
        this.turnaroundHours = turnaroundHours;
        this.dailyCapacity = dailyCapacity;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.fee = fee;
        this.building = building;
        this.floorLabel = floorLabel;
        this.roomNumber = roomNumber;
        this.active = true;
    }

    public Hospital getHospital() { return hospital; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public DiagnosticModality getModality() { return modality; }
    public String getPreparationInstructions() { return preparationInstructions; }
    public int getTurnaroundHours() { return turnaroundHours; }
    public int getDailyCapacity() { return dailyCapacity; }
    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public BigDecimal getFee() { return fee; }
    public String getBuilding() { return building; }
    public String getFloorLabel() { return floorLabel; }
    public String getRoomNumber() { return roomNumber; }
    public boolean isActive() { return active; }
}
