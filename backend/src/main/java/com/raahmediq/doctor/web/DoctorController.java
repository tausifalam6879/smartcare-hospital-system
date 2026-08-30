package com.raahmediq.doctor.web;

import com.raahmediq.common.web.PageResponse;
import com.raahmediq.doctor.service.DoctorService;
import com.raahmediq.doctor.web.DoctorDtos.DoctorRequest;
import com.raahmediq.doctor.web.DoctorDtos.DoctorResponse;
import com.raahmediq.doctor.web.DoctorDtos.ScheduleRequest;
import com.raahmediq.doctor.web.DoctorDtos.ScheduleResponse;
import com.raahmediq.doctor.web.DoctorDtos.LinkAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @GetMapping
    PageResponse<DoctorResponse> search(
            @RequestParam(required = false) UUID hospitalId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(hospitalId, departmentId, search, page, size);
    }

    @GetMapping("/{doctorId}")
    DoctorResponse get(@PathVariable UUID doctorId) {
        return service.get(doctorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    DoctorResponse create(@Valid @RequestBody DoctorRequest request) {
        return service.create(request);
    }

    @PutMapping("/{doctorId}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    DoctorResponse update(@PathVariable UUID doctorId, @Valid @RequestBody DoctorRequest request) {
        return service.update(doctorId, request);
    }

    @PostMapping("/{doctorId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    ScheduleResponse addSchedule(@PathVariable UUID doctorId, @Valid @RequestBody ScheduleRequest request) {
        return service.addSchedule(doctorId, request);
    }

    @DeleteMapping("/{doctorId}/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    void removeSchedule(@PathVariable UUID doctorId, @PathVariable UUID scheduleId) {
        service.removeSchedule(doctorId, scheduleId);
    }

    @PostMapping("/{doctorId}/account-link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    void linkAccount(@PathVariable UUID doctorId, @Valid @RequestBody LinkAccountRequest request) {
        service.linkAccount(doctorId, request);
    }
}
