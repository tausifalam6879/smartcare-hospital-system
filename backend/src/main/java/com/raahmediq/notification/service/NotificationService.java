package com.raahmediq.notification.service;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.audit.service.AuditService;
import com.raahmediq.common.error.NotFoundException;
import com.raahmediq.notification.channel.NotificationChannelAdapter;
import com.raahmediq.notification.config.NotificationProperties;
import com.raahmediq.notification.domain.DeliveryStatus;
import com.raahmediq.notification.domain.Notification;
import com.raahmediq.notification.domain.NotificationChannel;
import com.raahmediq.notification.domain.NotificationDelivery;
import com.raahmediq.notification.domain.NotificationType;
import com.raahmediq.notification.repository.NotificationDeliveryRepository;
import com.raahmediq.notification.repository.NotificationRepository;
import com.raahmediq.notification.web.NotificationDtos.NotificationResponse;
import com.raahmediq.patient.domain.Patient;
import com.raahmediq.patient.repository.PatientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationDeliveryRepository deliveries;
    private final PatientRepository patients;
    private final NotificationProperties properties;
    private final List<NotificationChannelAdapter> adapters;
    private final AuditService audit;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, NotificationDeliveryRepository deliveries,
                               PatientRepository patients, NotificationProperties properties,
                               List<NotificationChannelAdapter> adapters, AuditService audit, Clock clock) {
        this.notifications = notifications;
        this.deliveries = deliveries;
        this.patients = patients;
        this.properties = properties;
        this.adapters = adapters;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void notifyAppointment(Appointment appointment, NotificationType type, String eventKey,
                                  String title, String message) {
        String deduplicationKey = type + ":" + appointment.getId() + ":" + eventKey;
        if (notifications.findByDeduplicationKey(deduplicationKey).isPresent()) return;
        Notification notification = notifications.save(new Notification(appointment.getPatient(), appointment, type,
                title, message, deduplicationKey));
        for (NotificationChannel channel : properties.channels()) dispatch(notification, channel);
        audit.recordAs("SYSTEM", "NOTIFICATION_CREATED", "NOTIFICATION", notification.getId(),
                appointment.getHospital().getId());
    }

    @Transactional
    public void notifyPatient(Patient patient, UUID hospitalId, NotificationType type, UUID resourceId,
                              String eventKey, String title, String message) {
        String deduplicationKey = type + ":" + resourceId + ":" + eventKey;
        if (notifications.findByDeduplicationKey(deduplicationKey).isPresent()) return;
        Notification notification = notifications.save(new Notification(patient, null, type,
                title, message, deduplicationKey));
        for (NotificationChannel channel : properties.channels()) dispatch(notification, channel);
        audit.recordAs("SYSTEM", "NOTIFICATION_CREATED", "NOTIFICATION", notification.getId(), hospitalId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        return notifications.findAllByPatientIdOrderByCreatedAtDesc(patient.getId(), PageRequest.of(0, 100)).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByPatientIdAndReadAtIsNull(requirePatient(userId).getId());
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Patient patient = requirePatient(userId);
        Notification notification = notifications.findByIdAndPatientId(notificationId, patient.getId())
                .orElseThrow(() -> new NotFoundException("Notification was not found."));
        notification.markRead(clock.instant());
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        Patient patient = requirePatient(userId);
        notifications.findAllByPatientIdOrderByCreatedAtDesc(patient.getId(), PageRequest.of(0, 500))
                .forEach(notification -> notification.markRead(clock.instant()));
    }

    private void dispatch(Notification notification, NotificationChannel channel) {
        NotificationChannelAdapter adapter = adapters.stream().filter(item -> item.supports(channel)).findFirst()
                .orElse(null);
        NotificationChannelAdapter.DeliveryOutcome outcome = adapter == null
                ? new NotificationChannelAdapter.DeliveryOutcome(DeliveryStatus.SKIPPED_NOT_CONFIGURED,
                "No adapter is registered for " + channel + ".")
                : adapter.deliver(notification, channel);
        deliveries.save(new NotificationDelivery(notification, channel, outcome.status(), outcome.detail(),
                clock.instant()));
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required for this operation."));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(),
                notification.getAppointment() == null ? null : notification.getAppointment().getId(),
                notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.getReadAt() != null, notification.getReadAt(), notification.getCreatedAt());
    }
}
