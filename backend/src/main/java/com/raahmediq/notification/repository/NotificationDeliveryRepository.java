package com.raahmediq.notification.repository;

import com.raahmediq.notification.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import com.raahmediq.notification.domain.DeliveryStatus;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    long countByStatus(DeliveryStatus status);
}
