package com.raahmediq.diagnostic.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "diagnostic_day_ledgers", uniqueConstraints = @UniqueConstraint(
        name = "uk_diagnostic_day_ledger", columnNames = {"procedure_id", "service_date"}))
public class DiagnosticDayLedger extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedure_id", nullable = false)
    private DiagnosticProcedure procedure;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "effective_capacity", nullable = false)
    private int effectiveCapacity;

    @Column(name = "active_count", nullable = false)
    private int activeCount;

    @Column(name = "next_position", nullable = false)
    private int nextPosition;

    protected DiagnosticDayLedger() {
    }

    public DiagnosticDayLedger(DiagnosticProcedure procedure, LocalDate serviceDate, int effectiveCapacity) {
        this.procedure = procedure;
        this.serviceDate = serviceDate;
        this.effectiveCapacity = effectiveCapacity;
        this.nextPosition = 1;
    }

    public Integer allocate() {
        if (activeCount >= effectiveCapacity) return null;
        activeCount++;
        return nextPosition++;
    }

    public void release() {
        if (activeCount > 0) activeCount--;
    }

    public int getEffectiveCapacity() { return effectiveCapacity; }
    public int getActiveCount() { return activeCount; }
}
