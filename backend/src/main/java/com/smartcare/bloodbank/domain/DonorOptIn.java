package com.smartcare.bloodbank.domain;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "blood_donor_opt_ins")
public class DonorOptIn extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_blood_group", length = 20)
    private BloodGroup verifiedBloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_status", nullable = false, length = 30)
    private DonorEligibilityStatus eligibilityStatus;

    @Column(name = "contact_preference", nullable = false, length = 30)
    private String contactPreference;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;

    @Column(name = "eligibility_verified_at")
    private Instant eligibilityVerifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private UserAccount verifiedBy;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected DonorOptIn() {
    }

    public DonorOptIn(UserAccount user, String contactPreference, Instant consentedAt) {
        this.user = user;
        this.contactPreference = contactPreference;
        this.consentedAt = consentedAt;
        this.eligibilityStatus = DonorEligibilityStatus.PENDING_VERIFICATION;
    }

    public void renewConsent(String preference, Instant at) {
        contactPreference = preference;
        consentedAt = at;
        withdrawnAt = null;
        eligibilityStatus = DonorEligibilityStatus.PENDING_VERIFICATION;
        verifiedBloodGroup = null;
        eligibilityVerifiedAt = null;
        verifiedBy = null;
    }

    public void verify(BloodGroup bloodGroup, DonorEligibilityStatus status, Instant at, UserAccount verifier) {
        if (status != DonorEligibilityStatus.ELIGIBLE
                && status != DonorEligibilityStatus.TEMPORARILY_INELIGIBLE) {
            throw new IllegalStateException("Staff verification must set an eligibility decision.");
        }
        verifiedBloodGroup = bloodGroup;
        eligibilityStatus = status;
        eligibilityVerifiedAt = at;
        verifiedBy = verifier;
    }

    public void withdraw(Instant at) {
        eligibilityStatus = DonorEligibilityStatus.WITHDRAWN;
        withdrawnAt = at;
    }

    public UserAccount getUser() { return user; }
    public BloodGroup getVerifiedBloodGroup() { return verifiedBloodGroup; }
    public DonorEligibilityStatus getEligibilityStatus() { return eligibilityStatus; }
    public String getContactPreference() { return contactPreference; }
    public Instant getConsentedAt() { return consentedAt; }
    public Instant getEligibilityVerifiedAt() { return eligibilityVerifiedAt; }
    public UserAccount getVerifiedBy() { return verifiedBy; }
    public Instant getWithdrawnAt() { return withdrawnAt; }
}
