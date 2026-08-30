package com.raahmediq.auth.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserAccount extends AuditableEntity {

    @Column(name = "mobile_number", nullable = false, unique = true, length = 20)
    private String mobileNumber;

    @Column(unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    protected UserAccount() {
    }

    public UserAccount(String mobileNumber, String email, String passwordHash, String displayName,
                       String preferredLanguage, Set<Role> roles) {
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.preferredLanguage = preferredLanguage;
        this.status = AccountStatus.ACTIVE;
        this.roles = EnumSet.copyOf(roles);
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void grantRole(Role role) {
        roles.add(role);
    }
}
