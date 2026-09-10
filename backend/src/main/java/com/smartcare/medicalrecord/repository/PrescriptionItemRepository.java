package com.smartcare.medicalrecord.repository;

import com.smartcare.medicalrecord.domain.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
    List<PrescriptionItem> findAllByPrescriptionIdOrderByItemOrderAsc(UUID prescriptionId);
}
