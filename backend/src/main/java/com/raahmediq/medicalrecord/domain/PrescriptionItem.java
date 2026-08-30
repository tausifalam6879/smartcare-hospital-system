package com.raahmediq.medicalrecord.domain;

import com.raahmediq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "medicine_name", nullable = false, length = 180)
    private String medicineName;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(nullable = false, length = 140)
    private String frequency;

    @Column(name = "duration_text", nullable = false, length = 140)
    private String durationText;

    @Column(length = 80)
    private String route;

    @Column(length = 500)
    private String instructions;

    protected PrescriptionItem() {
    }

    public PrescriptionItem(Prescription prescription, int itemOrder, String medicineName, String dosage,
                            String frequency, String durationText, String route, String instructions) {
        this.prescription = prescription;
        this.itemOrder = itemOrder;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.durationText = durationText;
        this.route = route;
        this.instructions = instructions;
    }

    public int getItemOrder() { return itemOrder; }
    public String getMedicineName() { return medicineName; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public String getDurationText() { return durationText; }
    public String getRoute() { return route; }
    public String getInstructions() { return instructions; }
}
