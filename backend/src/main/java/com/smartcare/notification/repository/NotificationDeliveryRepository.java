package com.smartcare.notification.repository;

import com.smartcare.notification.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import com.smartcare.notification.domain.DeliveryStatus;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    long countByStatus(DeliveryStatus status);
}
