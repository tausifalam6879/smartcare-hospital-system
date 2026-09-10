package com.smartcare.notification.channel;

import com.smartcare.notification.domain.DeliveryStatus;
import com.smartcare.notification.domain.Notification;
import com.smartcare.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentExternalNotificationAdapter implements NotificationChannelAdapter {
    @Override
    public boolean supports(NotificationChannel channel) {
        return channel != NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryOutcome deliver(Notification notification, NotificationChannel channel) {
        return new DeliveryOutcome(DeliveryStatus.SKIPPED_NOT_CONFIGURED,
                channel + " provider credentials are not configured; no external delivery was claimed.");
    }
}
