package com.smartcare.notification.channel;

import com.smartcare.notification.domain.DeliveryStatus;
import com.smartcare.notification.domain.Notification;
import com.smartcare.notification.domain.NotificationChannel;

public interface NotificationChannelAdapter {
    boolean supports(NotificationChannel channel);
    DeliveryOutcome deliver(Notification notification, NotificationChannel channel);

    record DeliveryOutcome(DeliveryStatus status, String detail) {
    }
}
