package com.smartcare.payment.web;

import com.smartcare.payment.service.PaymentService;
import com.smartcare.payment.web.PaymentDtos.IntentRequest;
import com.smartcare.payment.web.PaymentDtos.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    public PaymentResponse createIntent(@AuthenticationPrincipal Jwt jwt,
                                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @Valid @RequestBody IntentRequest request) {
        return service.createIntent(UUID.fromString(jwt.getSubject()), request.appointmentId(), idempotencyKey);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<PaymentResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/webhooks/{provider}")
    public PaymentResponse webhook(@PathVariable String provider,
                                   @RequestHeader("X-Payment-Event-Id") String providerEventId,
                                   @RequestHeader("X-Payment-Signature") String signature,
                                   @RequestBody String rawBody) {
        return service.processWebhook(provider, providerEventId, signature, rawBody);
    }
}
