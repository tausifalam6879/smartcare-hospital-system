package com.smartcare.doctor.service;

import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.common.web.PageResponse;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.domain.DoctorSchedule;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.doctor.repository.DoctorScheduleRepository;
import com.smartcare.doctor.web.DoctorDtos.DoctorRequest;
import com.smartcare.doctor.web.DoctorDtos.DoctorResponse;
import com.smartcare.doctor.web.DoctorDtos.ScheduleRequest;
import com.smartcare.doctor.web.DoctorDtos.ScheduleResponse;
import com.smartcare.doctor.web.DoctorDtos.LinkAccountRequest;
import com.smartcare.hospital.domain.Department;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.service.HospitalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final HospitalService hospitals;
    private final AuditService audit;
    private final UserAccountRepository users;

    public DoctorService(DoctorRepository doctors, DoctorScheduleRepository schedules, HospitalService hospitals,
                         AuditService audit, UserAccountRepository users) {
        this.doctors = doctors;
        this.schedules = schedules;
        this.hospitals = hospitals;
        this.audit = audit;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> search(UUID hospitalId, UUID departmentId, String search, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Specification<Doctor> specification = (root, query, builder) -> builder.isTrue(root.get("active"));
        if (hospitalId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("hospital").get("id"), hospitalId));
        }
        if (departmentId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("department").get("id"), departmentId));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("specialization")), pattern)));
        }
        Page<DoctorResponse> result = doctors.findAll(specification,
                        PageRequest.of(safePage, safeSize, Sort.by("name").ascending()))
                .map(doctor -> toResponse(doctor, List.of()));
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public DoctorResponse get(UUID doctorId) {
        Doctor doctor = requireDoctor(doctorId);
        return toResponse(doctor, schedules.findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId));
    }

    @Transactional
    public DoctorResponse create(DoctorRequest request) {
        Hospital hospital = hospitals.requireHospital(request.hospitalId());
        Department department = hospitals.requireDepartment(request.hospitalId(), request.departmentId());
        String registration = request.registrationNumber().trim().toUpperCase(Locale.ROOT);
        if (doctors.existsByRegistrationNumberIgnoreCase(registration)) {
            throw new ConflictException("A doctor already uses this registration number.");
        }
        Doctor doctor = doctors.save(new Doctor(hospital, department, request.name().trim(),
                request.specialization().trim(), registration, request.consultationFee(),
                request.expectedConsultationMinutes(), request.dailyMaxCapacity(), blankToNull(request.building()),
                blankToNull(request.floorLabel()), blankToNull(request.roomNumber())));
        audit.record("DOCTOR_CREATED", "DOCTOR", doctor.getId(), hospital.getId());
        return toResponse(doctor, List.of());
    }

    @Transactional
    public DoctorResponse update(UUID doctorId, DoctorRequest request) {
        Doctor doctor = requireDoctor(doctorId);
        if (!doctor.getHospital().getId().equals(request.hospitalId())) {
            throw new IllegalArgumentException("A doctor cannot be moved across hospitals by this operation.");
        }
        Department department = hospitals.requireDepartment(request.hospitalId(), request.departmentId());
        String registration = request.registrationNumber().trim().toUpperCase(Locale.ROOT);
        if (!doctor.getRegistrationNumber().equalsIgnoreCase(registration)
                && doctors.existsByRegistrationNumberIgnoreCase(registration)) {
            throw new ConflictException("A doctor already uses this registration number.");
        }
        doctor.update(department, request.name().trim(), request.specialization().trim(), registration,
                request.consultationFee(), request.expectedConsultationMinutes(), request.dailyMaxCapacity(),
                blankToNull(request.building()), blankToNull(request.floorLabel()), blankToNull(request.roomNumber()),
                request.active() == null || request.active());
        audit.record("DOCTOR_UPDATED", "DOCTOR", doctor.getId(), doctor.getHospital().getId());
        return toResponse(doctor, schedules.findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId));
    }

    @Transactional
    public ScheduleResponse addSchedule(UUID doctorId, ScheduleRequest request) {
        Doctor doctor = requireDoctor(doctorId);
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Schedule startTime must be before endTime.");
        }
        boolean overlaps = schedules.findAllByDoctorIdAndDayOfWeek(doctorId, request.dayOfWeek()).stream()
                .anyMatch(existing -> request.startTime().isBefore(existing.getEndTime())
                        && request.endTime().isAfter(existing.getStartTime()));
        if (overlaps) {
            throw new ConflictException("This schedule overlaps an existing schedule for the doctor.");
        }
        DoctorSchedule schedule = schedules.save(new DoctorSchedule(doctor, request.dayOfWeek(), request.startTime(),
                request.endTime(), request.slotDurationMinutes(), request.capacityOverride()));
        audit.record("DOCTOR_SCHEDULE_CREATED", "DOCTOR_SCHEDULE", schedule.getId(), doctor.getHospital().getId());
        return toResponse(schedule);
    }

    @Transactional
    public void removeSchedule(UUID doctorId, UUID scheduleId) {
        DoctorSchedule schedule = schedules.findById(scheduleId)
                .filter(found -> found.getDoctor().getId().equals(doctorId))
                .orElseThrow(() -> new NotFoundException("Doctor schedule was not found."));
        audit.record("DOCTOR_SCHEDULE_DELETED", "DOCTOR_SCHEDULE", schedule.getId(),
                schedule.getDoctor().getHospital().getId());
        schedules.delete(schedule);
    }

    @Transactional
    public void linkAccount(UUID doctorId, LinkAccountRequest request) {
        Doctor doctor = requireDoctor(doctorId);
        UserAccount user = users.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User account was not found."));
        doctors.findByLinkedUserId(user.getId()).filter(existing -> !existing.getId().equals(doctorId))
                .ifPresent(existing -> { throw new ConflictException("This account is linked to another doctor."); });
        user.grantRole(Role.DOCTOR);
        doctor.linkAccount(user);
        audit.record("DOCTOR_ACCOUNT_LINKED", "DOCTOR", doctor.getId(), doctor.getHospital().getId());
    }

    private Doctor requireDoctor(UUID id) {
        return doctors.findById(id).orElseThrow(() -> new NotFoundException("Doctor was not found."));
    }

    private static DoctorResponse toResponse(Doctor doctor, List<DoctorSchedule> schedules) {
        return new DoctorResponse(doctor.getId(), doctor.getHospital().getId(), doctor.getHospital().getName(),
                doctor.getDepartment().getId(), doctor.getDepartment().getName(), doctor.getName(),
                doctor.getSpecialization(), doctor.getRegistrationNumber(), doctor.getConsultationFee(),
                doctor.getExpectedConsultationMinutes(), doctor.getDailyMaxCapacity(), doctor.getBuilding(),
                doctor.getFloorLabel(), doctor.getRoomNumber(), doctor.isActive(),
                schedules.stream().map(DoctorService::toResponse).toList());
    }

    private static ScheduleResponse toResponse(DoctorSchedule schedule) {
        return new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(), schedule.getStartTime(),
                schedule.getEndTime(), schedule.getSlotDurationMinutes(), schedule.getCapacityOverride());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
