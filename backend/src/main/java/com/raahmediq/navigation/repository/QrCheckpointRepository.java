package com.raahmediq.navigation.repository;

import com.raahmediq.navigation.domain.QrCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrCheckpointRepository extends JpaRepository<QrCheckpoint, UUID> {
    Optional<QrCheckpoint> findByPublicCodeIgnoreCaseAndActiveTrue(String publicCode);
    List<QrCheckpoint> findAllByHospitalIdAndActiveTrueOrderByLabelEnAsc(UUID hospitalId);
}
