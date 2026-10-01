package com.smartcare.hospital.web;

import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.DepartmentResponse;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
public class HospitalController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.smartcare.auth.repository.UserAccountRepository users;
    @org.springframework.beans.factory.annotation.Autowired
    private com.smartcare.doctor.repository.DoctorRepository doctors;
    @org.springframework.beans.factory.annotation.Autowired
    private com.smartcare.auth.service.HospitalAccess hospitalAccess;

    private final HospitalService service;

    public HospitalController(HospitalService service) {
        this.service = service;
    }

    @GetMapping
    List<HospitalResponse> list() {
        return service.listActive();
    }
    @GetMapping("/assigned")
    @PreAuthorize("isAuthenticated()")
    List<HospitalResponse> assigned(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        var id = UUID.fromString(jwt.getSubject());
        var account = users.findById(id).orElseThrow();
        if (account.getRoles().contains(com.smartcare.auth.domain.Role.SUPER_ADMIN)) return service.listActive();
        var ids = new java.util.HashSet<>(account.getHospitalIds());
        doctors.findByLinkedUserId(id).filter(d -> d.isActive()).ifPresent(d -> ids.add(d.getHospital().getId()));
        return service.listActive().stream().filter(h -> ids.contains(h.id())).toList();
    }

    @GetMapping("/{hospitalId}")
    HospitalResponse get(@PathVariable UUID hospitalId) {
        return service.get(hospitalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    HospitalResponse create(@Valid @RequestBody HospitalRequest request) {
        return service.create(request);
    }

    @PutMapping("/{hospitalId}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    HospitalResponse update(@PathVariable UUID hospitalId, @Valid @RequestBody HospitalRequest request) {
        hospitalAccess.requireCurrent(hospitalId);
        return service.update(hospitalId, request);
    }

    @GetMapping("/{hospitalId}/departments")
    List<DepartmentResponse> departments(@PathVariable UUID hospitalId) {
        return service.listDepartments(hospitalId);
    }

    @PostMapping("/{hospitalId}/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    DepartmentResponse createDepartment(@PathVariable UUID hospitalId,
                                        @Valid @RequestBody DepartmentRequest request) {
        hospitalAccess.requireCurrent(hospitalId);
        return service.createDepartment(hospitalId, request);
    }
}
