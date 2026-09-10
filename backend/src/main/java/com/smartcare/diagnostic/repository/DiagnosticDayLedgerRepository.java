package com.smartcare.diagnostic.repository;

import com.smartcare.diagnostic.domain.DiagnosticDayLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticDayLedgerRepository extends JpaRepository<DiagnosticDayLedger, UUID> {
    Optional<DiagnosticDayLedger> findByProcedureIdAndServiceDate(UUID procedureId, LocalDate serviceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ledger from DiagnosticDayLedger ledger where ledger.procedure.id = :procedureId " +
            "and ledger.serviceDate = :serviceDate")
    Optional<DiagnosticDayLedger> findForUpdate(@Param("procedureId") UUID procedureId,
                                                @Param("serviceDate") LocalDate serviceDate);
}
