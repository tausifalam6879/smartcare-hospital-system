package com.smartcare.auth.domain;
import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="account_recovery_codes")
public class AccountRecovery extends AuditableEntity {
    @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="used_at") private Instant usedAt;
    protected AccountRecovery() {}
    public AccountRecovery(String hash, UUID userId, Instant expires) { this.tokenHash=hash; this.userId=userId; this.expiresAt=expires; }
    public UUID getUserId() { return userId; }
    public boolean usable(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
    public void consume(Instant now) { usedAt=now; }
}
