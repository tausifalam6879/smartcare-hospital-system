package com.raahmediq.operations.domain;

import com.raahmediq.auth.domain.UserAccount;
import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.doctor.domain.Doctor;
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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "doctor_day_operations", uniqueConstraints = @UniqueConstraint(
        name = "uk_doctor_day_operation", columnNames = {"doctor_id", "service_date"}))
public class DoctorDayOperation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DoctorDayStatus status;

    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes;

    @Column(length = 300)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false)
    private UserAccount updatedBy;

    @Column(name = "operational_updated_at", nullable = false)
    private Instant operationalUpdatedAt;

    protected DoctorDayOperation() {
    }

    public DoctorDayOperation(Doctor doctor, LocalDate serviceDate, DoctorDayStatus status,
                              String reason, UserAccount updatedBy, Instant at) {
        this.doctor = doctor;
        this.hospital = doctor.getHospital();
        this.serviceDate = serviceDate;
        update(status, reason, updatedBy, at);
    }

    public void update(DoctorDayStatus status, String reason, UserAccount updatedBy, Instant at) {
        this.status = status;
        this.delayMinutes = switch (status) {
            case DELAYED_30 -> 30;
            case DELAYED_60 -> 60;
            default -> 0;
        };
        this.reason = reason;
        this.updatedBy = updatedBy;
        this.operationalUpdatedAt = at;
    }

    public Doctor getDoctor() { return doctor; }
    public Hospital getHospital() { return hospital; }
    public LocalDate getServiceDate() { return serviceDate; }
    public DoctorDayStatus getStatus() { return status; }
    public int getDelayMinutes() { return delayMinutes; }
    public String getReason() { return reason; }
    public UserAccount getUpdatedBy() { return updatedBy; }
    public Instant getOperationalUpdatedAt() { return operationalUpdatedAt; }
}
