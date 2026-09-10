package com.smartcare.diagnostic.repository;

import com.smartcare.diagnostic.domain.DiagnosticModality;
import com.smartcare.diagnostic.domain.DiagnosticProcedure;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticProcedureRepository extends JpaRepository<DiagnosticProcedure, UUID> {
    List<DiagnosticProcedure> findAllByHospitalIdAndActiveTrueOrderByNameAsc(UUID hospitalId);
    List<DiagnosticProcedure> findAllByHospitalIdAndModalityAndActiveTrueOrderByNameAsc(
            UUID hospitalId, DiagnosticModality modality);
    boolean existsByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select procedure from DiagnosticProcedure procedure where procedure.id = :id")
    Optional<DiagnosticProcedure> findByIdForUpdate(@Param("id") UUID id);
}
