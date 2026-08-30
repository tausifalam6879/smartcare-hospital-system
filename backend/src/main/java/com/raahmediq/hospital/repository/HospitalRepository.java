package com.raahmediq.hospital.repository;

import com.raahmediq.hospital.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Hospital> findByCodeIgnoreCase(String code);
    List<Hospital> findAllByActiveTrueOrderByNameAsc();
}
