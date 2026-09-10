package com.smartcare.ambulance.web;

import com.smartcare.ambulance.domain.AmbulanceRequestStatus;
import com.smartcare.ambulance.service.AmbulanceService;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceAvailabilityResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceRequestResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AmbulanceResponse;
import com.smartcare.ambulance.web.AmbulanceDtos.AssignAmbulance;
import com.smartcare.ambulance.web.AmbulanceDtos.CancelAmbulanceRequest;
import com.smartcare.ambulance.web.AmbulanceDtos.CreateAmbulance;
import com.smartcare.ambulance.web.AmbulanceDtos.CreateAmbulanceRequest;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceAvailability;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceLocation;
import com.smartcare.ambulance.web.AmbulanceDtos.UpdateAmbulanceRequestStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AmbulanceController {
    private final AmbulanceService service;

    public AmbulanceController(AmbulanceService service) {
        this.service = service;
    }

    @GetMapping("/ambulances/availability")
    @PreAuthorize("isAuthenticated()")
    public AmbulanceAvailabilityResponse availability(@RequestParam UUID hospitalId) {
        return service.availability(hospitalId);
    }

    @GetMapping("/ambulances")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<AmbulanceResponse> fleet(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID hospitalId) {
        return service.fleet(subject(jwt), hospitalId);
    }

    @PostMapping("/ambulances")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceResponse createAmbulance(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateAmbulance request) {
        return service.createAmbulance(subject(jwt), request);
    }

    @PostMapping("/ambulances/{ambulanceId}/location")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceResponse updateLocation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ambulanceId,
                                            @Valid @RequestBody UpdateAmbulanceLocation request) {
        return service.updateLocation(subject(jwt), ambulanceId, request);
    }

    @PostMapping("/ambulances/{ambulanceId}/availability")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceResponse updateAvailability(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID ambulanceId,
                                                @Valid @RequestBody UpdateAmbulanceAvailability request) {
        return service.updateAvailability(subject(jwt), ambulanceId, request);
    }

    @PostMapping("/ambulance-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','RECEPTIONIST','BLOOD_BANK_STAFF','AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceRequestResponse createRequest(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateAmbulanceRequest request) {
        return service.createRequest(subject(jwt), request);
    }

    @GetMapping("/ambulance-requests/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AmbulanceRequestResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(subject(jwt));
    }

    @GetMapping("/ambulance-requests")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<AmbulanceRequestResponse> worklist(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam UUID hospitalId,
                                                  @RequestParam(required = false) AmbulanceRequestStatus status) {
        return service.worklist(subject(jwt), hospitalId, status);
    }

    @PostMapping("/ambulance-requests/{requestId}/assign")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceRequestResponse assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
                                           @Valid @RequestBody AssignAmbulance request) {
        return service.assign(subject(jwt), requestId, request);
    }

    @PostMapping("/ambulance-requests/{requestId}/acknowledge")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceRequestResponse acknowledge(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return service.acknowledge(subject(jwt), requestId);
    }

    @PostMapping("/ambulance-requests/{requestId}/status")
    @PreAuthorize("hasAnyRole('AMBULANCE_DISPATCHER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AmbulanceRequestResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
                                                 @Valid @RequestBody UpdateAmbulanceRequestStatus request) {
        return service.updateStatus(subject(jwt), requestId, request);
    }

    @PostMapping("/ambulance-requests/{requestId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public AmbulanceRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
                                           @Valid @RequestBody CancelAmbulanceRequest request) {
        return service.cancel(subject(jwt), requestId, request);
    }

    private static UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
