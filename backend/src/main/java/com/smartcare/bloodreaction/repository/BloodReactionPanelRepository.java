package com.smartcare.bloodreaction.repository;

import com.smartcare.bloodreaction.domain.BloodReactionPanel;
import com.smartcare.bloodreaction.domain.BloodReactionPanelStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BloodReactionPanelRepository extends JpaRepository<BloodReactionPanel, UUID> {
    List<BloodReactionPanel> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<BloodReactionPanel> findAllByOrderByCreatedAtDesc();
    List<BloodReactionPanel> findAllByStatusOrderByCreatedAtAsc(BloodReactionPanelStatus status);
}
