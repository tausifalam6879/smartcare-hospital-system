package com.raahmediq.notification.channel;

import com.raahmediq.notification.domain.DeliveryStatus;
import com.raahmediq.notification.domain.Notification;
import com.raahmediq.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannelAdapter implements NotificationChannelAdapter {
    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryOutcome deliver(Notification notification, NotificationChannel channel) {
        return new DeliveryOutcome(DeliveryStatus.DELIVERED, "Stored in the authenticated RaahMediQ Health inbox.");
    }
}
