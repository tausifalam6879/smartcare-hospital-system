package com.smartcare.notification.repository;

import com.smartcare.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);
    long countByPatientIdAndReadAtIsNull(UUID patientId);
    Optional<Notification> findByIdAndPatientId(UUID id, UUID patientId);
    Optional<Notification> findByDeduplicationKey(String deduplicationKey);
}
