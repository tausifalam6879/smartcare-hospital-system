package com.smartcare.navigation.domain;

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

@Entity
@Table(name = "hospital_locations", uniqueConstraints =
        @UniqueConstraint(name = "uk_hospital_location_code", columnNames = {"hospital_id", "code"}))
public class HospitalLocation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "name_en", nullable = false, length = 140)
    private String nameEn;

    @Column(name = "name_hi", nullable = false, length = 180)
    private String nameHi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LocationType type;

    @Column(nullable = false, length = 80)
    private String building;

    @Column(name = "floor_label", nullable = false, length = 40)
    private String floorLabel;

    @Column(length = 80)
    private String zone;

    @Column(name = "room_number", length = 40)
    private String roomNumber;

    @Column(name = "map_x", nullable = false)
    private int mapX;

    @Column(name = "map_y", nullable = false)
    private int mapY;

    @Column(nullable = false)
    private boolean active;

    protected HospitalLocation() {
    }

    public HospitalLocation(Hospital hospital, String code, String nameEn, String nameHi, LocationType type,
                            String building, String floorLabel, String zone, String roomNumber, int mapX, int mapY) {
        this.hospital = hospital;
        this.code = code;
        this.nameEn = nameEn;
        this.nameHi = nameHi;
        this.type = type;
        this.building = building;
        this.floorLabel = floorLabel;
        this.zone = zone;
        this.roomNumber = roomNumber;
        this.mapX = mapX;
        this.mapY = mapY;
        this.active = true;
    }

    public Hospital getHospital() { return hospital; }
    public String getCode() { return code; }
    public String getNameEn() { return nameEn; }
    public String getNameHi() { return nameHi; }
    public LocationType getType() { return type; }
    public String getBuilding() { return building; }
    public String getFloorLabel() { return floorLabel; }
    public String getZone() { return zone; }
    public String getRoomNumber() { return roomNumber; }
    public int getMapX() { return mapX; }
    public int getMapY() { return mapY; }
    public boolean isActive() { return active; }
}
