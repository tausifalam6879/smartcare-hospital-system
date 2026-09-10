package com.smartcare.doctor.repository;

import com.smartcare.doctor.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID>, JpaSpecificationExecutor<Doctor> {
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
    Optional<Doctor> findByRegistrationNumberIgnoreCase(String registrationNumber);
    Optional<Doctor> findFirstByHospitalIdAndNameIgnoreCase(UUID hospitalId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") UUID id);

    Optional<Doctor> findByLinkedUserId(UUID userId);
    List<Doctor> findAllByHospitalIdAndActiveTrueOrderByNameAsc(UUID hospitalId);
}
