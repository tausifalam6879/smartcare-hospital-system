package com.smartcare.diagnostic.domain;

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
@Table(name = "diagnostic_results")
public class DiagnosticResult extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private DiagnosticOrder order;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(length = 6000)
    private String findings;

    @Column(length = 3000)
    private String impression;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_flag", nullable = false, length = 20)
    private DiagnosticResultFlag overallFlag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verified_by_user_id", nullable = false)
    private UserAccount verifiedBy;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    protected DiagnosticResult() {
    }

    public DiagnosticResult(DiagnosticOrder order, String summary, String findings, String impression,
                            DiagnosticResultFlag overallFlag, UserAccount verifiedBy, Instant verifiedAt) {
        this.order = order;
        this.summary = summary;
        this.findings = findings;
        this.impression = impression;
        this.overallFlag = overallFlag;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
    }

    public String getSummary() { return summary; }
    public String getFindings() { return findings; }
    public String getImpression() { return impression; }
    public DiagnosticResultFlag getOverallFlag() { return overallFlag; }
    public UserAccount getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
