package com.smartcare.checkin.domain;

import com.smartcare.appointment.domain.Appointment;
import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
public class CheckIn extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CheckInChannel channel;

    @Column(name = "privacy_token", nullable = false, unique = true, length = 24)
    private String privacyToken;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @Column(name = "verified_by_user_id", nullable = false)
    private UUID verifiedByUserId;

    protected CheckIn() {
    }

    public CheckIn(Appointment appointment, CheckInChannel channel, String privacyToken,
                   Instant checkedInAt, UUID verifiedByUserId) {
        this.appointment = appointment;
        this.channel = channel;
        this.privacyToken = privacyToken;
        this.checkedInAt = checkedInAt;
        this.verifiedByUserId = verifiedByUserId;
    }

    public Appointment getAppointment() { return appointment; }
    public CheckInChannel getChannel() { return channel; }
    public String getPrivacyToken() { return privacyToken; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public UUID getVerifiedByUserId() { return verifiedByUserId; }
}
