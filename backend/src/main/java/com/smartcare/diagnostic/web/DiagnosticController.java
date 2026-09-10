package com.smartcare.diagnostic.web;

import com.smartcare.diagnostic.domain.DiagnosticModality;
import com.smartcare.diagnostic.service.DiagnosticWorkflowService;
import com.smartcare.diagnostic.web.DiagnosticDtos.AvailabilityResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.CancelOrderRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.CreateOrderRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.OrderResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.ProcedureRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.ProcedureResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.ScheduleOrderRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.VerifyResultRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticController {
    private final DiagnosticWorkflowService service;

    public DiagnosticController(DiagnosticWorkflowService service) {
        this.service = service;
    }

    @GetMapping("/procedures")
    public List<ProcedureResponse> procedures(@RequestParam UUID hospitalId,
                                              @RequestParam(required = false) DiagnosticModality modality) {
        return service.procedures(hospitalId, modality);
    }

    @GetMapping("/procedures/{procedureId}/availability")
    public AvailabilityResponse availability(@PathVariable UUID procedureId,
                                             @RequestParam LocalDate date) {
        return service.availability(procedureId, date);
    }

    @PostMapping("/procedures")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ProcedureResponse createProcedure(@Valid @RequestBody ProcedureRequest request) {
        return service.createProcedure(request);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    public OrderResponse createOrder(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody CreateOrderRequest request) {
        return service.createOrder(subject(jwt), request);
    }

    @GetMapping("/orders/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(subject(jwt));
    }

    @PostMapping("/orders/{orderId}/schedule")
    @PreAuthorize("hasRole('PATIENT')")
    public OrderResponse schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
                                  @Valid @RequestBody ScheduleOrderRequest request) {
        return service.schedule(subject(jwt), orderId, request);
    }

    @PostMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
                                @Valid @RequestBody CancelOrderRequest request) {
        return service.cancel(subject(jwt), orderId, request);
    }

    @GetMapping("/worklist")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<OrderResponse> worklist(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID hospitalId,
                                        @RequestParam LocalDate date) {
        return service.worklist(subject(jwt), hospitalId, date);
    }

    @PostMapping("/orders/{orderId}/collect")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public OrderResponse collect(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return service.collect(subject(jwt), orderId);
    }

    @PostMapping("/orders/{orderId}/start")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public OrderResponse start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return service.start(subject(jwt), orderId);
    }

    @PostMapping("/orders/{orderId}/verify-result")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public OrderResponse verifyResult(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
                                      @Valid @RequestBody VerifyResultRequest request) {
        return service.verifyResult(subject(jwt), orderId, request);
    }

    private static UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
