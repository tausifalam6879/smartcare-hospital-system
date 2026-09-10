package com.smartcare.followup.web;

import com.smartcare.followup.service.CareFollowUpService;
import com.smartcare.followup.web.FollowUpDtos.FollowUpResponse;
import com.smartcare.followup.web.FollowUpDtos.StatusUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/follow-ups")
@PreAuthorize("hasRole('PATIENT')")
public class CareFollowUpController {
    private final CareFollowUpService service;

    public CareFollowUpController(CareFollowUpService service) { this.service = service; }

    @GetMapping("/mine")
    public List<FollowUpResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/{followUpId}/status")
    public FollowUpResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID followUpId,
                                         @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(UUID.fromString(jwt.getSubject()), followUpId, request.status());
    }
}
