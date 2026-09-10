package com.smartcare.hospital.repository;

import com.smartcare.hospital.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Hospital> findByCodeIgnoreCase(String code);
    Optional<Hospital> findFirstByNameIgnoreCase(String name);
    List<Hospital> findAllByActiveTrueOrderByNameAsc();
}
