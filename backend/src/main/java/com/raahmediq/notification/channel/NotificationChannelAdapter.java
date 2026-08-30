package com.raahmediq.notification.channel;

import com.raahmediq.notification.domain.DeliveryStatus;
import com.raahmediq.notification.domain.Notification;
import com.raahmediq.notification.domain.NotificationChannel;

public interface NotificationChannelAdapter {
    boolean supports(NotificationChannel channel);
    DeliveryOutcome deliver(Notification notification, NotificationChannel channel);

    record DeliveryOutcome(DeliveryStatus status, String detail) {
    }
}
