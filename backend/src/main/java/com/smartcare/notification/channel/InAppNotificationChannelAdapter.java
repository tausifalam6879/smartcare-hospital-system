package com.smartcare.notification.channel;

import com.smartcare.notification.domain.DeliveryStatus;
import com.smartcare.notification.domain.Notification;
import com.smartcare.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannelAdapter implements NotificationChannelAdapter {
    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryOutcome deliver(Notification notification, NotificationChannel channel) {
        return new DeliveryOutcome(DeliveryStatus.DELIVERED, "Stored in the authenticated SmartCare inbox.");
    }
}
