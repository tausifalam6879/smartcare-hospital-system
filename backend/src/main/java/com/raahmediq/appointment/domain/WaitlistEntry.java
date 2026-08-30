package com.raahmediq.appointment.domain;

import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.doctor.domain.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "waitlist_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_waitlist_appointment", columnNames = "appointment_id"),
        @UniqueConstraint(name = "uk_waitlist_sequence", columnNames = {"doctor_id", "service_date", "sequence_number"})
})
public class WaitlistEntry extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status;

    @Column(name = "promoted_at")
    private Instant promotedAt;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(Appointment appointment, long sequenceNumber) {
        this.appointment = appointment;
        this.doctor = appointment.getDoctor();
        this.serviceDate = appointment.getServiceDate();
        this.sequenceNumber = sequenceNumber;
        this.status = WaitlistStatus.WAITING;
    }

    public void promote(Instant at) {
        status = WaitlistStatus.PROMOTED;
        promotedAt = at;
    }

    public void cancel() {
        status = WaitlistStatus.CANCELLED;
    }

    public Appointment getAppointment() { return appointment; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getServiceDate() { return serviceDate; }
    public long getSequenceNumber() { return sequenceNumber; }
    public WaitlistStatus getStatus() { return status; }
    public Instant getPromotedAt() { return promotedAt; }
}
