package com.smartcare.auth.service;
import com.smartcare.auth.domain.*;
import com.smartcare.auth.repository.*;
import com.smartcare.auth.security.AuthAttemptLimiter;
import com.smartcare.audit.service.AuditService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
@Service
public class AccountRecoveryService {
    private final AccountRecoveryRepository codes;
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final AuditService audit;
    private final AuthAttemptLimiter attempts;
    private final Clock clock;
    public AccountRecoveryService(AccountRecoveryRepository codes, UserAccountRepository users, PasswordEncoder passwords,
            AuditService audit, AuthAttemptLimiter attempts, Clock clock) {
        this.codes=codes; this.users=users; this.passwords=passwords; this.audit=audit; this.attempts=attempts; this.clock=clock;
    }
    public record Issued(String code, Instant expiresAt) {}
    @Transactional @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Issued issue(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (user.getRoles().contains(Role.SUPER_ADMIN) || user.getStatus()!=AccountStatus.ACTIVE)
            throw new AccessDeniedException("Privileged/inactive account recovery requires the offline administrator procedure.");
        codes.findAllByUserId(userId).forEach(c -> c.consume(clock.instant()));
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String code=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry=clock.instant().plusSeconds(1800);
        codes.save(new AccountRecovery(hash(code),userId,expiry));
        audit.record("RECOVERY_CODE_ISSUED_AFTER_IDENTITY_CHECK", "USER", userId, null);
        return new Issued(code,expiry);
    }
    @Transactional
    public void recover(String mobile, String code, String password) {
        attempts.check("recovery:"+mobile.trim());
        var recovery=codes.findLocked(hash(code.trim())).orElseThrow(() -> new AccessDeniedException("Invalid recovery code."));
        var user=users.findById(recovery.getUserId()).orElseThrow();
        if (!recovery.usable(clock.instant()) || !user.getMobileNumber().equals(mobile.trim()) || user.getStatus()!=AccountStatus.ACTIVE || user.getRoles().contains(Role.SUPER_ADMIN))
            throw new AccessDeniedException("Invalid recovery code.");
        if(password.length()<12 || password.length()>72) throw new IllegalArgumentException("Password must contain 12–72 characters.");
        user.changePassword(passwords.encode(password));
        recovery.consume(clock.instant());
        audit.recordAs(user.getId().toString(),"ACCOUNT_RECOVERED_SESSIONS_REVOKED","USER",user.getId(),null);
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
