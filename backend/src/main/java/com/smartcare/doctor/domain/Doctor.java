package com.smartcare.doctor.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.hospital.domain.Department;
import com.smartcare.hospital.domain.Hospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
public class Doctor extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(nullable = false, length = 140)
    private String specialization;

    @Column(name = "registration_number", nullable = false, unique = true, length = 60)
    private String registrationNumber;

    @Column(name = "consultation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "expected_consultation_minutes", nullable = false)
    private int expectedConsultationMinutes;

    @Column(name = "daily_max_capacity", nullable = false)
    private int dailyMaxCapacity;

    @Column(length = 80)
    private String building;

    @Column(name = "floor_label", length = 40)
    private String floorLabel;

    @Column(name = "room_number", length = 40)
    private String roomNumber;

    @Column(nullable = false)
    private boolean active;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id", unique = true)
    private UserAccount linkedUser;

    protected Doctor() {
    }

    public Doctor(Hospital hospital, Department department, String name, String specialization,
                  String registrationNumber, BigDecimal consultationFee, int expectedConsultationMinutes,
                  int dailyMaxCapacity, String building, String floorLabel, String roomNumber) {
        this.hospital = hospital;
        this.department = department;
        apply(name, specialization, registrationNumber, consultationFee, expectedConsultationMinutes,
                dailyMaxCapacity, building, floorLabel, roomNumber, true);
    }

    public void update(Department department, String name, String specialization, String registrationNumber,
                       BigDecimal consultationFee, int expectedConsultationMinutes, int dailyMaxCapacity,
                       String building, String floorLabel, String roomNumber, boolean active) {
        this.department = department;
        apply(name, specialization, registrationNumber, consultationFee, expectedConsultationMinutes,
                dailyMaxCapacity, building, floorLabel, roomNumber, active);
    }

    private void apply(String name, String specialization, String registrationNumber, BigDecimal consultationFee,
                       int expectedConsultationMinutes, int dailyMaxCapacity, String building,
                       String floorLabel, String roomNumber, boolean active) {
        this.name = name;
        this.specialization = specialization;
        this.registrationNumber = registrationNumber;
        this.consultationFee = consultationFee;
        this.expectedConsultationMinutes = expectedConsultationMinutes;
        this.dailyMaxCapacity = dailyMaxCapacity;
        this.building = building;
        this.floorLabel = floorLabel;
        this.roomNumber = roomNumber;
        this.active = active;
    }

    public Hospital getHospital() { return hospital; }
    public Department getDepartment() { return department; }
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public String getRegistrationNumber() { return registrationNumber; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public int getExpectedConsultationMinutes() { return expectedConsultationMinutes; }
    public int getDailyMaxCapacity() { return dailyMaxCapacity; }
    public String getBuilding() { return building; }
    public String getFloorLabel() { return floorLabel; }
    public String getRoomNumber() { return roomNumber; }
    public boolean isActive() { return active; }
    public UserAccount getLinkedUser() { return linkedUser; }

    public void linkAccount(UserAccount user) {
        this.linkedUser = user;
    }
}
