package com.raahmediq.bloodbank.domain;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.auth.domain.UserAccount;
import com.raahmediq.common.domain.AuditableEntity;
import com.raahmediq.hospital.domain.Hospital;
import com.raahmediq.patient.domain.Patient;
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

@Entity
@Table(name = "blood_requests", uniqueConstraints = @UniqueConstraint(
        name = "uk_blood_request_idempotency", columnNames = {"created_by_user_id", "idempotency_key"}))
public class BloodRequest extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserAccount createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 20)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BloodComponent component;

    @Column(name = "requested_units", nullable = false)
    private int requestedUnits;

    @Column(name = "matched_units", nullable = false)
    private int matchedUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BloodUrgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BloodRequestStatus status;

    @Column(name = "clinical_reason", nullable = false, length = 2000)
    private String clinicalReason;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected BloodRequest() {
    }

    public BloodRequest(Patient patient, Hospital hospital, Appointment appointment, UserAccount createdBy,
                        BloodGroup bloodGroup, BloodComponent component, int requestedUnits,
                        BloodUrgency urgency, String clinicalReason, String idempotencyKey) {
        this.patient = patient;
        this.hospital = hospital;
        this.appointment = appointment;
        this.createdBy = createdBy;
        this.bloodGroup = bloodGroup;
        this.component = component;
        this.requestedUnits = requestedUnits;
        this.matchedUnits = 0;
        this.urgency = urgency;
        this.status = BloodRequestStatus.SEARCHING;
        this.clinicalReason = clinicalReason;
        this.idempotencyKey = idempotencyKey;
    }

    public void addMatchedUnits(int units) {
        if (units <= 0 || matchedUnits + units > requestedUnits) {
            throw new IllegalStateException("Invalid blood-unit allocation.");
        }
        matchedUnits += units;
        status = matchedUnits == requestedUnits ? BloodRequestStatus.RESERVED
                : BloodRequestStatus.PARTIALLY_RESERVED;
    }

    public void markUnavailable() {
        if (matchedUnits == 0) status = BloodRequestStatus.UNAVAILABLE;
        else if (matchedUnits < requestedUnits) status = BloodRequestStatus.PARTIALLY_RESERVED;
    }

    public void fulfil(Instant at) {
        if (status != BloodRequestStatus.RESERVED || matchedUnits != requestedUnits) {
            throw new IllegalStateException("Only a fully reserved blood request can be fulfilled.");
        }
        status = BloodRequestStatus.FULFILLED;
        resolvedAt = at;
    }

    public void cancel(Instant at, String reason) {
        if (status == BloodRequestStatus.FULFILLED || status == BloodRequestStatus.CANCELLED) {
            throw new IllegalStateException("This blood request cannot be cancelled.");
        }
        status = BloodRequestStatus.CANCELLED;
        matchedUnits = 0;
        resolvedAt = at;
        cancellationReason = reason;
    }

    public Patient getPatient() { return patient; }
    public Hospital getHospital() { return hospital; }
    public Appointment getAppointment() { return appointment; }
    public UserAccount getCreatedBy() { return createdBy; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public BloodComponent getComponent() { return component; }
    public int getRequestedUnits() { return requestedUnits; }
    public int getMatchedUnits() { return matchedUnits; }
    public BloodUrgency getUrgency() { return urgency; }
    public BloodRequestStatus getStatus() { return status; }
    public String getClinicalReason() { return clinicalReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getCancellationReason() { return cancellationReason; }
}
