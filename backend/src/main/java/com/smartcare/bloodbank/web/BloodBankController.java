package com.smartcare.bloodbank.web;

import com.smartcare.bloodbank.domain.BloodComponent;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodbank.domain.BloodRequestStatus;
import com.smartcare.bloodbank.service.BloodBankService;
import com.smartcare.bloodbank.web.BloodBankDtos.AvailabilityResponse;
import com.smartcare.bloodbank.web.BloodBankDtos.BloodBankRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.BloodBankResponse;
import com.smartcare.bloodbank.web.BloodBankDtos.BloodRequestResponse;
import com.smartcare.bloodbank.web.BloodBankDtos.CancelBloodRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.CreateBloodRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.DonorConsentRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.DonorMatchResponse;
import com.smartcare.bloodbank.web.BloodBankDtos.DonorResponse;
import com.smartcare.bloodbank.web.BloodBankDtos.DonorVerificationRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.InventoryRequest;
import com.smartcare.bloodbank.web.BloodBankDtos.InventoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/v1")
public class BloodBankController {
    private final BloodBankService service;

    public BloodBankController(BloodBankService service) {
        this.service = service;
    }

    @GetMapping("/blood-banks")
    @PreAuthorize("isAuthenticated()")
    public List<BloodBankResponse> banks(@RequestParam UUID hospitalId) {
        return service.banks(hospitalId);
    }

    @PostMapping("/blood-banks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public BloodBankResponse createBank(@Valid @RequestBody BloodBankRequest request) {
        return service.createBank(request);
    }

    @GetMapping("/blood-banks/availability")
    @PreAuthorize("isAuthenticated()")
    public List<AvailabilityResponse> availability(@RequestParam UUID hospitalId,
                                                   @RequestParam BloodGroup bloodGroup,
                                                   @RequestParam BloodComponent component,
                                                   @RequestParam(defaultValue = "1") @Min(1) @Max(20) int units) {
        return service.availability(hospitalId, bloodGroup, component, units);
    }

    @GetMapping("/blood-banks/inventory")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<InventoryResponse> inventory(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam UUID hospitalId,
                                             @RequestParam BloodGroup bloodGroup,
                                             @RequestParam BloodComponent component) {
        return service.inventory(subject(jwt), hospitalId, bloodGroup, component);
    }

    @PostMapping("/blood-banks/inventory")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public InventoryResponse verifyInventory(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody InventoryRequest request) {
        return service.verifyInventory(subject(jwt), request);
    }

    @PostMapping("/blood-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public BloodRequestResponse createRequest(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody CreateBloodRequest request) {
        return service.createRequest(subject(jwt), request);
    }

    @GetMapping("/blood-requests/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<BloodRequestResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(subject(jwt));
    }

    @GetMapping("/blood-requests")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<BloodRequestResponse> worklist(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam UUID hospitalId,
                                              @RequestParam(required = false) BloodRequestStatus status) {
        return service.worklist(subject(jwt), hospitalId, status);
    }

    @PostMapping("/blood-requests/{requestId}/search")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public BloodRequestResponse searchAgain(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return service.searchAgain(subject(jwt), requestId);
    }

    @PostMapping("/blood-requests/{requestId}/fulfil")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public BloodRequestResponse fulfil(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return service.fulfil(subject(jwt), requestId);
    }

    @PostMapping("/blood-requests/{requestId}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public BloodRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
                                       @Valid @RequestBody CancelBloodRequest request) {
        return service.cancel(subject(jwt), requestId, request);
    }

    @GetMapping("/blood-donors/me")
    @PreAuthorize("isAuthenticated()")
    public DonorResponse donorMine(@AuthenticationPrincipal Jwt jwt) {
        return service.donorMine(subject(jwt));
    }

    @PostMapping("/blood-donors/consent")
    @PreAuthorize("isAuthenticated()")
    public DonorResponse consent(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody DonorConsentRequest request) {
        return service.consent(subject(jwt), request);
    }

    @PostMapping("/blood-donors/withdraw")
    @PreAuthorize("isAuthenticated()")
    public DonorResponse withdraw(@AuthenticationPrincipal Jwt jwt) {
        return service.withdrawConsent(subject(jwt));
    }

    @PostMapping("/blood-donors/{donorId}/verify")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public DonorResponse verifyDonor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID donorId,
                                     @Valid @RequestBody DonorVerificationRequest request) {
        return service.verifyDonor(subject(jwt), donorId, request);
    }

    @GetMapping("/blood-requests/{requestId}/donor-matches")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<DonorMatchResponse> donorMatches(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable UUID requestId) {
        return service.donorMatches(subject(jwt), requestId);
    }

    private static UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
