package com.smartcare.ambulance.domain;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ambulance_request_events")
public class AmbulanceRequestEvent extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambulance_request_id", nullable = false)
    private AmbulanceRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private UserAccount actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private AmbulanceRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private AmbulanceRequestStatus toStatus;

    @Column(length = 300)
    private String note;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    protected AmbulanceRequestEvent() {
    }

    public AmbulanceRequestEvent(AmbulanceRequest request, UserAccount actor, AmbulanceRequestStatus fromStatus,
                                 AmbulanceRequestStatus toStatus, String note, Instant eventAt) {
        this.request = request;
        this.actor = actor;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.eventAt = eventAt;
    }

    public AmbulanceRequest getRequest() { return request; }
    public UserAccount getActor() { return actor; }
    public AmbulanceRequestStatus getFromStatus() { return fromStatus; }
    public AmbulanceRequestStatus getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public Instant getEventAt() { return eventAt; }
}
