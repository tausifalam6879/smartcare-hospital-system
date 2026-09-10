package com.smartcare.appointment.domain;

import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.doctor.domain.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "doctor_day_ledgers", uniqueConstraints = @UniqueConstraint(
        name = "uk_doctor_day_ledger", columnNames = {"doctor_id", "service_date"}))
public class DoctorDayLedger extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "effective_capacity", nullable = false)
    private int effectiveCapacity;

    @Column(name = "active_count", nullable = false)
    private int activeCount;

    @Column(name = "next_position", nullable = false)
    private int nextPosition;

    protected DoctorDayLedger() {
    }

    public DoctorDayLedger(Doctor doctor, LocalDate serviceDate, int effectiveCapacity) {
        this.doctor = doctor;
        this.serviceDate = serviceDate;
        this.effectiveCapacity = effectiveCapacity;
        this.activeCount = 0;
        this.nextPosition = 1;
    }

    public Integer allocate() {
        if (activeCount >= effectiveCapacity) return null;
        activeCount++;
        return nextPosition++;
    }

    public void release() {
        if (activeCount > 0) activeCount--;
    }

    public Doctor getDoctor() { return doctor; }
    public LocalDate getServiceDate() { return serviceDate; }
    public int getEffectiveCapacity() { return effectiveCapacity; }
    public int getActiveCount() { return activeCount; }
    public int getNextPosition() { return nextPosition; }
}
