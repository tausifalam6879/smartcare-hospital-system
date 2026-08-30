package com.raahmediq.queue.web;

import com.raahmediq.queue.service.QueueService;
import com.raahmediq.queue.web.QueueDtos.PatientQueueResponse;
import com.raahmediq.queue.web.QueueDtos.PublicQueueResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queues")
public class QueueController {
    private final QueueService service;

    public QueueController(QueueService service) {
        this.service = service;
    }

    @GetMapping("/{doctorId}/live")
    public PublicQueueResponse live(@PathVariable UUID doctorId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.publicSnapshot(doctorId, date);
    }

    @GetMapping("/appointments/{appointmentId}")
    public PatientQueueResponse patient(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId) {
        return service.patientSnapshot(UUID.fromString(jwt.getSubject()), appointmentId);
    }

    @PostMapping("/{doctorId}/serve-next")
    public PublicQueueResponse serveNext(@PathVariable UUID doctorId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.serveNext(doctorId, date);
    }
}
