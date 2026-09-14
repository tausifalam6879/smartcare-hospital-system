package com.smartcare.checkin.web;

import com.smartcare.checkin.service.CheckInService;
import com.smartcare.checkin.web.CheckInDtos.CheckInRequest;
import com.smartcare.checkin.web.CheckInDtos.CheckInResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/check-in/appointments")
public class CheckInController {
    private final CheckInService service;

    public CheckInController(CheckInService service) {
        this.service = service;
    }

    @PostMapping("/{appointmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public CheckInResponse checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId,
                                   @Valid @RequestBody CheckInRequest request) {
        return service.checkIn(UUID.fromString(jwt.getSubject()), roles(jwt), appointmentId, request.channel());
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public CheckInResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId) {
        return service.get(UUID.fromString(jwt.getSubject()), roles(jwt), appointmentId);
    }

    private static List<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles == null ? List.of() : roles;
    }
}
