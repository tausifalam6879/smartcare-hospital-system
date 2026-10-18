package com.smartcare.hospital.repository;

import com.smartcare.hospital.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select h from Hospital h where h.id = :id")
    Optional<Hospital> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    boolean existsByCodeIgnoreCase(String code);
    Optional<Hospital> findByCodeIgnoreCase(String code);
    Optional<Hospital> findFirstByNameIgnoreCase(String name);
    List<Hospital> findAllByActiveTrueOrderByNameAsc();
}
