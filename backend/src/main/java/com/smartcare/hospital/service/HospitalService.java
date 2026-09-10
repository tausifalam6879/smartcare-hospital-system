package com.smartcare.hospital.service;

import com.smartcare.audit.service.AuditService;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.hospital.domain.Department;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.repository.DepartmentRepository;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.hospital.web.HospitalDtos.DepartmentRequest;
import com.smartcare.hospital.web.HospitalDtos.DepartmentResponse;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.hospital.web.HospitalDtos.HospitalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HospitalService {

    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final AuditService audit;

    public HospitalService(HospitalRepository hospitals, DepartmentRepository departments, AuditService audit) {
        this.hospitals = hospitals;
        this.departments = departments;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> listActive() {
        return hospitals.findAllByActiveTrueOrderByNameAsc().stream()
                .map(hospital -> toResponse(hospital, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HospitalResponse get(UUID hospitalId) {
        Hospital hospital = requireHospital(hospitalId);
        return toResponse(hospital, listDepartments(hospitalId));
    }

    @Transactional
    public HospitalResponse create(HospitalRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (hospitals.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A hospital already uses code " + code + ".");
        }
        validateZone(request.timeZone());
        Hospital hospital = hospitals.save(new Hospital(code, request.name().trim(), request.addressLine().trim(),
                request.city().trim(), request.state().trim(), request.postalCode().trim(),
                request.contactNumber().trim(), request.timeZone().trim()));
        audit.record("HOSPITAL_CREATED", "HOSPITAL", hospital.getId(), hospital.getId());
        return toResponse(hospital, List.of());
    }

    @Transactional
    public HospitalResponse update(UUID hospitalId, HospitalRequest request) {
        Hospital hospital = requireHospital(hospitalId);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        hospitals.findByCodeIgnoreCase(code).filter(found -> !found.getId().equals(hospitalId)).ifPresent(found -> {
            throw new ConflictException("A hospital already uses code " + code + ".");
        });
        validateZone(request.timeZone());
        hospital.update(code, request.name().trim(), request.addressLine().trim(), request.city().trim(),
                request.state().trim(), request.postalCode().trim(), request.contactNumber().trim(),
                request.timeZone().trim(), request.active() == null || request.active());
        audit.record("HOSPITAL_UPDATED", "HOSPITAL", hospital.getId(), hospital.getId());
        return toResponse(hospital, listDepartments(hospitalId));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments(UUID hospitalId) {
        requireHospital(hospitalId);
        return departments.findAllByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId).stream()
                .map(HospitalService::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(UUID hospitalId, DepartmentRequest request) {
        Hospital hospital = requireHospital(hospitalId);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (departments.existsByHospitalIdAndCodeIgnoreCase(hospitalId, code)) {
            throw new ConflictException("This hospital already has a department with code " + code + ".");
        }
        Department department = departments.save(new Department(hospital, code, request.name().trim(),
                blankToNull(request.description())));
        audit.record("DEPARTMENT_CREATED", "DEPARTMENT", department.getId(), hospitalId);
        return toResponse(department);
    }

    public Hospital requireHospital(UUID id) {
        return hospitals.findById(id).orElseThrow(() -> new NotFoundException("Hospital was not found."));
    }

    public Department requireDepartment(UUID hospitalId, UUID departmentId) {
        return departments.findByIdAndHospitalId(departmentId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Department was not found in this hospital."));
    }

    private HospitalResponse toResponse(Hospital hospital, List<DepartmentResponse> departmentResponses) {
        return new HospitalResponse(hospital.getId(), hospital.getCode(), hospital.getName(),
                hospital.getAddressLine(), hospital.getCity(), hospital.getState(), hospital.getPostalCode(),
                hospital.getContactNumber(), hospital.getTimeZone(), hospital.isActive(), departmentResponses);
    }

    private static DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getCode(), department.getName(),
                department.getDescription());
    }

    private static void validateZone(String value) {
        try {
            ZoneId.of(value.trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("timeZone must be a valid IANA time zone, such as Asia/Kolkata.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
