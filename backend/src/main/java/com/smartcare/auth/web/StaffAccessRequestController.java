package com.smartcare.auth.web;

import com.smartcare.auth.service.StaffAccessRequestService;
import com.smartcare.auth.security.AuthAttemptLimiter;
import com.smartcare.auth.web.StaffAccessDtos.*;
import com.smartcare.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/staff-access-requests")
public class StaffAccessRequestController {
    private final StaffAccessRequestService service;
    private final AuthAttemptLimiter attempts;
    public StaffAccessRequestController(StaffAccessRequestService service, AuthAttemptLimiter attempts) {
        this.service = service; this.attempts = attempts;
    }
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public void submit(@Valid @RequestBody RequestStaffAccess input, jakarta.servlet.http.HttpServletRequest http) {
        attempts.check("staff-request-ip:" + http.getRemoteAddr());
        attempts.check("staff-request-mobile:" + input.mobileNumber().trim());
        service.submit(input);
    }
    @GetMapping
    public PageResponse<AccessRequestSummary> pending(@RequestParam UUID hospitalId,
            @RequestParam(defaultValue = "0") int page) { return service.pending(hospitalId, page); }
    @PostMapping("/{id}/reject") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID id) { service.reject(id); }
}
