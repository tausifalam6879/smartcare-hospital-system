package com.smartcare.auth.web;

import com.smartcare.auth.service.StaffAccessService;
import com.smartcare.auth.web.StaffAccessDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class StaffAccessController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.smartcare.auth.security.AuthAttemptLimiter attempts;
    private final StaffAccessService service;
    public StaffAccessController(StaffAccessService service) { this.service = service; }
    @GetMapping("/staff")
    public java.util.List<StaffAccessService.StaffAccount> directory() { return service.directory(); }
    @PostMapping("/staff-invitations") @ResponseStatus(HttpStatus.CREATED)
    public InvitationIssued issue(@Valid @RequestBody IssueInvitation input) { return service.issue(input); }
    @DeleteMapping("/staff-invitations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) { service.revoke(id); }
    @PutMapping("/staff/{userId}/hospitals/{hospitalId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable UUID userId, @PathVariable UUID hospitalId) { service.assignHospital(userId, hospitalId, true); }
    @DeleteMapping("/staff/{userId}/hospitals/{hospitalId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable UUID userId, @PathVariable UUID hospitalId) { service.assignHospital(userId, hospitalId, false); }
    @PostMapping("/register-staff") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse enroll(@Valid @RequestBody StaffRegistration input, jakarta.servlet.http.HttpServletRequest http) {
        attempts.check("staff-signup-ip:" + http.getRemoteAddr());
        return service.enroll(input);
    }
}
