package com.raahmediq.operations.web;

import com.raahmediq.operations.service.HospitalOperationsService;
import com.raahmediq.operations.web.OperationsDtos.DoctorDayStatusResponse;
import com.raahmediq.operations.web.OperationsDtos.OperationsDashboardResponse;
import com.raahmediq.operations.web.OperationsDtos.RecoveryCaseResponse;
import com.raahmediq.operations.web.OperationsDtos.ResolveRecovery;
import com.raahmediq.operations.web.OperationsDtos.UpdateDoctorDayStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationsController {
    private final HospitalOperationsService service;

    public OperationsController(HospitalOperationsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','CASHIER','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public OperationsDashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam UUID hospitalId,
                                                  @RequestParam LocalDate date) {
        return service.dashboard(subject(jwt), hospitalId, date);
    }

    @PostMapping("/doctor-status")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public DoctorDayStatusResponse updateDoctorStatus(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody UpdateDoctorDayStatus request) {
        return service.updateDoctorStatus(subject(jwt), request);
    }

    @PostMapping("/appointments/{appointmentId}/no-show")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public void noShow(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId) {
        service.markNoShow(subject(jwt), appointmentId);
    }

    @GetMapping("/recovery/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<RecoveryCaseResponse> recoveryMine(@AuthenticationPrincipal Jwt jwt) {
        return service.recoveryMine(subject(jwt));
    }

    @PostMapping("/recovery/{caseId}/decision")
    @PreAuthorize("hasRole('PATIENT')")
    public RecoveryCaseResponse resolveRecovery(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID caseId,
                                                @Valid @RequestBody ResolveRecovery request) {
        return service.resolveRecovery(subject(jwt), caseId, request);
    }

    private static UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
