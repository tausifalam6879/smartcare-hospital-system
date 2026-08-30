package com.raahmediq.diagnostic.repository;

import com.raahmediq.diagnostic.domain.DiagnosticOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticOrderRepository extends JpaRepository<DiagnosticOrder, UUID> {
    List<DiagnosticOrder> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);
    Optional<DiagnosticOrder> findByIdAndPatientId(UUID id, UUID patientId);
    boolean existsByAppointmentIdAndProcedureId(UUID appointmentId, UUID procedureId);
    List<DiagnosticOrder> findAllByProcedureHospitalIdAndScheduledDateOrderByQueuePositionAsc(
            UUID hospitalId, LocalDate scheduledDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select diagnosticOrder from DiagnosticOrder diagnosticOrder where diagnosticOrder.id = :id")
    Optional<DiagnosticOrder> findByIdForUpdate(@Param("id") UUID id);
}
