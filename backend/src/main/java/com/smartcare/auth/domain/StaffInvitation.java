package com.smartcare.auth.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_invitations")
public class StaffInvitation extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false, length = 20) private String mobileNumber;
    @Column(nullable = false, length = 40) private String accountType;
    @Column(nullable = false) private UUID hospitalId;
    private UUID doctorId;
    @Column(nullable = false) private Instant expiresAt;
    private Instant consumedAt;
    private Instant revokedAt;
    protected StaffInvitation() {}
    public StaffInvitation(String hash, String mobile, String type, UUID hospital, UUID doctor, Instant expiry) {
        tokenHash = hash; mobileNumber = mobile; accountType = type; hospitalId = hospital;
        doctorId = doctor; expiresAt = expiry;
    }
    public String getMobileNumber() { return mobileNumber; }
    public String getAccountType() { return accountType; }
    public UUID getHospitalId() { return hospitalId; }
    public UUID getDoctorId() { return doctorId; }
    public boolean usable(Instant now) { return consumedAt == null && revokedAt == null && expiresAt.isAfter(now); }
    public void consume(Instant now) { consumedAt = now; }
    public void revoke(Instant now) { revokedAt = now; }
}
