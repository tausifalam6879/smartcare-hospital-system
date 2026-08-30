package com.raahmediq.medicalrecord.repository;

import com.raahmediq.medicalrecord.domain.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
    List<PrescriptionItem> findAllByPrescriptionIdOrderByItemOrderAsc(UUID prescriptionId);
}
