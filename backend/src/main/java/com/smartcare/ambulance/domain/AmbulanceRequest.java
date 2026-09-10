package com.smartcare.ambulance.domain;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.common.domain.AuditableEntity;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.patient.domain.Patient;
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
@Table(name = "ambulance_requests", uniqueConstraints = @UniqueConstraint(
        name = "uk_ambulance_request_idempotency", columnNames = {"requested_by_user_id", "idempotency_key"}))
public class AmbulanceRequest extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private UserAccount requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, length = 30)
    private TransportType transportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbulancePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AmbulanceRequestStatus status;

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_landmark", length = 180)
    private String pickupLandmark;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "assistance_notes", length = 500)
    private String assistanceNotes;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "status_updated_at", nullable = false)
    private Instant statusUpdatedAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected AmbulanceRequest() {
    }

    public AmbulanceRequest(Patient patient, Hospital hospital, UserAccount requestedBy,
                            TransportType transportType, AmbulancePriority priority, String pickupAddress,
                            String pickupLandmark, String contactNumber, String assistanceNotes,
                            String idempotencyKey, Instant now) {
        this.patient = patient;
        this.hospital = hospital;
        this.requestedBy = requestedBy;
        this.transportType = transportType;
        this.priority = priority;
        this.pickupAddress = pickupAddress;
        this.pickupLandmark = pickupLandmark;
        this.contactNumber = contactNumber;
        this.assistanceNotes = assistanceNotes;
        this.idempotencyKey = idempotencyKey;
        this.status = AmbulanceRequestStatus.REQUESTED;
        this.statusUpdatedAt = now;
    }

    public void assign(Ambulance ambulance, Instant at) {
        requireStatus(AmbulanceRequestStatus.REQUESTED);
        this.ambulance = ambulance;
        status = AmbulanceRequestStatus.ASSIGNED;
        statusUpdatedAt = at;
        dispatchedAt = at;
    }

    public void acknowledge(Instant at) {
        requireStatus(AmbulanceRequestStatus.ASSIGNED);
        status = AmbulanceRequestStatus.ACKNOWLEDGED;
        statusUpdatedAt = at;
        acknowledgedAt = at;
    }

    public void advance(AmbulanceRequestStatus next, Instant at) {
        AmbulanceRequestStatus expected = switch (status) {
            case ACKNOWLEDGED -> AmbulanceRequestStatus.EN_ROUTE_TO_PATIENT;
            case EN_ROUTE_TO_PATIENT -> AmbulanceRequestStatus.PATIENT_PICKED_UP;
            case PATIENT_PICKED_UP -> AmbulanceRequestStatus.EN_ROUTE_TO_HOSPITAL;
            case EN_ROUTE_TO_HOSPITAL -> AmbulanceRequestStatus.ARRIVED;
            case ARRIVED -> AmbulanceRequestStatus.COMPLETED;
            default -> null;
        };
        if (next != expected) throw new IllegalStateException("Ambulance request cannot move from " + status + " to " + next + ".");
        status = next;
        statusUpdatedAt = at;
        if (next == AmbulanceRequestStatus.COMPLETED) completedAt = at;
    }

    public void cancel(Instant at, String reason) {
        if (status == AmbulanceRequestStatus.COMPLETED || status == AmbulanceRequestStatus.CANCELLED
                || status == AmbulanceRequestStatus.PATIENT_PICKED_UP
                || status == AmbulanceRequestStatus.EN_ROUTE_TO_HOSPITAL
                || status == AmbulanceRequestStatus.ARRIVED) {
            throw new IllegalStateException("This ambulance request cannot be cancelled at its current stage.");
        }
        status = AmbulanceRequestStatus.CANCELLED;
        statusUpdatedAt = at;
        cancelledAt = at;
        cancellationReason = reason;
    }

    private void requireStatus(AmbulanceRequestStatus expected) {
        if (status != expected) throw new IllegalStateException("Ambulance request is not " + expected + ".");
    }

    public Patient getPatient() { return patient; }
    public Hospital getHospital() { return hospital; }
    public UserAccount getRequestedBy() { return requestedBy; }
    public Ambulance getAmbulance() { return ambulance; }
    public TransportType getTransportType() { return transportType; }
    public AmbulancePriority getPriority() { return priority; }
    public AmbulanceRequestStatus getStatus() { return status; }
    public String getPickupAddress() { return pickupAddress; }
    public String getPickupLandmark() { return pickupLandmark; }
    public String getContactNumber() { return contactNumber; }
    public String getAssistanceNotes() { return assistanceNotes; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getStatusUpdatedAt() { return statusUpdatedAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
}
