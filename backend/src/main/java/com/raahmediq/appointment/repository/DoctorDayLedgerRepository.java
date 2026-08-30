package com.raahmediq.appointment.repository;

import com.raahmediq.appointment.domain.DoctorDayLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DoctorDayLedgerRepository extends JpaRepository<DoctorDayLedger, UUID> {

    Optional<DoctorDayLedger> findByDoctorIdAndServiceDate(UUID doctorId, LocalDate serviceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ledger from DoctorDayLedger ledger where ledger.doctor.id = :doctorId and ledger.serviceDate = :serviceDate")
    Optional<DoctorDayLedger> findForUpdate(@Param("doctorId") UUID doctorId,
                                            @Param("serviceDate") LocalDate serviceDate);
}
