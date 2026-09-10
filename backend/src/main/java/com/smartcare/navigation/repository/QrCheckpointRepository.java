package com.smartcare.navigation.repository;

import com.smartcare.navigation.domain.QrCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrCheckpointRepository extends JpaRepository<QrCheckpoint, UUID> {
    Optional<QrCheckpoint> findByPublicCodeIgnoreCaseAndActiveTrue(String publicCode);
    List<QrCheckpoint> findAllByHospitalIdAndActiveTrueOrderByLabelEnAsc(UUID hospitalId);
}
