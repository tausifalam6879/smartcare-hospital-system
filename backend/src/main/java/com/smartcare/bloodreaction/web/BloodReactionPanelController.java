package com.smartcare.bloodreaction.web;

import com.smartcare.bloodreaction.service.BloodReactionPanelService;
import com.smartcare.bloodreaction.web.BloodReactionPanelDtos.PanelResponse;
import com.smartcare.bloodreaction.web.BloodReactionPanelDtos.VerifyPanel;
import com.smartcare.bloodreaction.web.BloodReactionPanelDtos.RejectPanel;
import com.smartcare.bloodreaction.domain.BloodReactionPanelStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blood-reaction-panels")
public class BloodReactionPanelController {
    private final BloodReactionPanelService service;
    public BloodReactionPanelController(BloodReactionPanelService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public PanelResponse submit(@AuthenticationPrincipal Jwt jwt, @RequestPart MultipartFile antiAFile,
                                @RequestPart MultipartFile antiBFile, @RequestPart MultipartFile antiDFile) {
        return service.submit(UUID.fromString(jwt.getSubject()), antiAFile, antiBFile, antiDFile);
    }
    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<PanelResponse> mine(@AuthenticationPrincipal Jwt jwt) { return service.mine(UUID.fromString(jwt.getSubject())); }
    @GetMapping("/worklist")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<PanelResponse> worklist(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) BloodReactionPanelStatus status) {
        return service.worklist(UUID.fromString(jwt.getSubject()), status);
    }
    @PostMapping("/{panelId}/verify")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public PanelResponse verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID panelId, @RequestBody VerifyPanel input) {
        return service.verify(UUID.fromString(jwt.getSubject()), panelId, input.confirmedGroup(), input.note());
    }
    @PostMapping("/{panelId}/reject")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public PanelResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID panelId, @RequestBody RejectPanel input) {
        return service.reject(UUID.fromString(jwt.getSubject()), panelId, input.reason());
    }
}
