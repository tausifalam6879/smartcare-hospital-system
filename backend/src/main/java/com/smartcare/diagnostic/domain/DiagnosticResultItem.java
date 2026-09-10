package com.smartcare.diagnostic.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "diagnostic_result_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_diagnostic_result_item_order", columnNames = {"result_id", "item_order"}))
public class DiagnosticResultItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private DiagnosticResult result;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "result_value", nullable = false, length = 180)
    private String value;

    @Column(length = 80)
    private String unit;

    @Column(name = "reference_range", length = 180)
    private String referenceRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiagnosticResultFlag flag;

    protected DiagnosticResultItem() {
    }

    public DiagnosticResultItem(DiagnosticResult result, int itemOrder, String name, String value,
                                String unit, String referenceRange, DiagnosticResultFlag flag) {
        this.result = result;
        this.itemOrder = itemOrder;
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.flag = flag;
    }

    public String getName() { return name; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public String getReferenceRange() { return referenceRange; }
    public DiagnosticResultFlag getFlag() { return flag; }
}
