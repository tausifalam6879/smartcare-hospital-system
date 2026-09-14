package com.smartcare.appointment.web;

import com.smartcare.appointment.service.AppointmentService;
import com.smartcare.appointment.web.AppointmentDtos.AppointmentResponse;
import com.smartcare.appointment.web.AppointmentDtos.AvailabilityResponse;
import com.smartcare.appointment.web.AppointmentDtos.BookingRequest;
import com.smartcare.appointment.web.AppointmentDtos.CancelRequest;
import com.smartcare.payment.service.PaymentService;
import com.smartcare.payment.web.PaymentDtos.CashConfirmationResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService service;
    private final PaymentService payments;

    public AppointmentController(AppointmentService service, PaymentService payments) {
        this.service = service;
        this.payments = payments;
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam UUID doctorId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                             LocalDate date) {
        return service.availability(doctorId, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentResponse book(@AuthenticationPrincipal Jwt jwt,
                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody BookingRequest request) {
        return service.book(UUID.fromString(jwt.getSubject()), idempotencyKey, request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AppointmentResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/doctor/mine")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<AppointmentResponse> doctorMine(@AuthenticationPrincipal Jwt jwt) {
        return service.doctorMine(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId,
                                      @Valid @RequestBody(required = false) CancelRequest request) {
        return payments.cancelAndRefund(UUID.fromString(jwt.getSubject()), appointmentId,
                request == null ? null : request.reason());
    }

    @PostMapping("/{appointmentId}/cash-confirmation")
    @PreAuthorize("hasAnyRole('CASHIER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public CashConfirmationResponse confirmCash(@PathVariable UUID appointmentId) {
        return payments.confirmCash(appointmentId);
    }
}
