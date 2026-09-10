package com.smartcare.hospital.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "departments", uniqueConstraints =
        @UniqueConstraint(name = "uk_department_hospital_code", columnNames = {"hospital_id", "code"}))
public class Department extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

    protected Department() {
    }

    public Department(Hospital hospital, String code, String name, String description) {
        this.hospital = hospital;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = true;
    }

    public Hospital getHospital() { return hospital; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
