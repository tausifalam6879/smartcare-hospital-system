package com.smartcare.navigation.web;

import com.smartcare.navigation.service.NavigationService;
import com.smartcare.navigation.web.NavigationDtos.AppointmentDestinationResponse;
import com.smartcare.navigation.web.NavigationDtos.CheckpointRequest;
import com.smartcare.navigation.web.NavigationDtos.CheckpointResponse;
import com.smartcare.navigation.web.NavigationDtos.HospitalMapResponse;
import com.smartcare.navigation.web.NavigationDtos.LocationRequest;
import com.smartcare.navigation.web.NavigationDtos.LocationResponse;
import com.smartcare.navigation.web.NavigationDtos.PathRequest;
import com.smartcare.navigation.web.NavigationDtos.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/navigation")
public class NavigationController {
    private final NavigationService service;

    public NavigationController(NavigationService service) {
        this.service = service;
    }

    @GetMapping("/hospitals/{hospitalId}/map")
    public HospitalMapResponse map(@PathVariable UUID hospitalId) {
        return service.map(hospitalId);
    }

    @GetMapping("/hospitals/{hospitalId}/route")
    public RouteResponse route(@PathVariable UUID hospitalId,
                               @RequestParam String fromCheckpoint,
                               @RequestParam String destinationCode,
                               @RequestParam(defaultValue = "en") String language,
                               @RequestParam(defaultValue = "false") boolean stepFree) {
        return service.route(hospitalId, fromCheckpoint, destinationCode, language, stepFree);
    }

    @GetMapping("/checkpoints/{publicCode}")
    public CheckpointResponse checkpoint(@PathVariable String publicCode) {
        return service.checkpoint(publicCode);
    }

    @GetMapping("/appointments/{appointmentId}/destination")
    public AppointmentDestinationResponse appointmentDestination(@AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable UUID appointmentId) {
        return service.appointmentDestination(UUID.fromString(jwt.getSubject()), appointmentId);
    }

    @PostMapping("/hospitals/{hospitalId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse createLocation(@PathVariable UUID hospitalId,
                                           @Valid @RequestBody LocationRequest request) {
        return service.createLocation(hospitalId, request);
    }

    @PostMapping("/hospitals/{hospitalId}/paths")
    @ResponseStatus(HttpStatus.CREATED)
    public void createPath(@PathVariable UUID hospitalId, @Valid @RequestBody PathRequest request) {
        service.createPath(hospitalId, request);
    }

    @PostMapping("/hospitals/{hospitalId}/checkpoints")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckpointResponse createCheckpoint(@PathVariable UUID hospitalId,
                                               @Valid @RequestBody CheckpointRequest request) {
        return service.createCheckpoint(hospitalId, request);
    }
}
