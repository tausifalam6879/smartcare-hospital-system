package com.smartcare.auth.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "staff_access_requests")
public class StaffAccessRequest extends AuditableEntity {
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 20) private String mobileNumber;
    @Column(length = 254) private String email;
    @Column(nullable = false, length = 40) private String accountType;
    @Column(nullable = false) private UUID hospitalId;
    @Column(nullable = false, length = 500) private String message;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    private UUID invitationId;
    protected StaffAccessRequest() {}
    public StaffAccessRequest(String name, String mobile, String email, String type, UUID hospital, String message) {
        this.name = name; mobileNumber = mobile; this.email = email; accountType = type;
        hospitalId = hospital; this.message = message;
    }
    public String getName() { return name; }
    public String getMobileNumber() { return mobileNumber; }
    public String getEmail() { return email; }
    public String getAccountType() { return accountType; }
    public UUID getHospitalId() { return hospitalId; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public UUID getInvitationId() { return invitationId; }
    public void approve(UUID id) { status = "APPROVED"; invitationId = id; }
    public void reject() { status = "REJECTED"; }
}
