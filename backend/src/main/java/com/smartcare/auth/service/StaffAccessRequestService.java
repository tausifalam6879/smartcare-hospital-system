package com.smartcare.auth.service;

import com.smartcare.auth.domain.StaffAccessRequest;
import com.smartcare.auth.repository.StaffAccessRequestRepository;
import com.smartcare.auth.web.StaffAccessDtos.*;
import com.smartcare.audit.service.AuditService;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.common.error.*;
import com.smartcare.common.web.PageResponse;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StaffAccessRequestService {
    private final StaffAccessRequestRepository requests;
    private final HospitalRepository hospitals;
    private final AuditService audit;
    public StaffAccessRequestService(StaffAccessRequestRepository requests, HospitalRepository hospitals, AuditService audit) {
        this.requests = requests; this.hospitals = hospitals; this.audit = audit;
    }
    @Transactional
    public void submit(RequestStaffAccess input) {
        StaffAccessService.rolesFor(input.accountType());
        if (!hospitals.findByIdForUpdate(input.hospitalId()).map(h -> h.isActive()).orElse(false))
            throw new NotFoundException("Active hospital was not found.");
        // Retries receive the same acknowledgement without exposing another request's details.
        if (requests.existsByHospitalIdAndMobileNumberAndAccountTypeAndStatus(
                input.hospitalId(), input.mobileNumber().trim(), input.accountType(), "PENDING")) return;
        requests.save(new StaffAccessRequest(input.name().trim(), input.mobileNumber().trim(),
                input.email() == null || input.email().isBlank() ? null : input.email().trim().toLowerCase(Locale.ROOT),
                input.accountType(), input.hospitalId(), input.message().trim()));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<AccessRequestSummary> pending(UUID hospitalId, int page) {
        return PageResponse.from(requests.findAllByHospitalIdAndStatus(hospitalId, "PENDING",
                PageRequest.of(Math.max(page, 0), 20, Sort.by("createdAt").ascending().and(Sort.by("id"))))
                .map(r -> new AccessRequestSummary(r.getId(), r.getName(), r.getMobileNumber(), r.getEmail(),
                        r.getAccountType(), r.getHospitalId(), r.getMessage(), r.getCreatedAt())));
    }
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void reject(UUID id) {
        var request = requests.findLockedById(id).orElseThrow(() -> new NotFoundException("Staff request was not found."));
        if (!request.getStatus().equals("PENDING")) throw new ConflictException("This request has already been reviewed.");
        request.reject();
        audit.record("STAFF_ACCESS_REQUEST_REJECTED", "STAFF_ACCESS_REQUEST", id, request.getHospitalId());
    }
}
