package com.smartcare.hospital.repository;

import com.smartcare.hospital.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    boolean existsByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);
    List<Department> findAllByHospitalIdAndActiveTrueOrderByNameAsc(UUID hospitalId);
    Optional<Department> findByIdAndHospitalId(UUID id, UUID hospitalId);
    Optional<Department> findByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);
}
