package com.raahmediq.operations.repository;

import com.raahmediq.operations.domain.AppointmentRecoveryCase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRecoveryCaseRepository extends JpaRepository<AppointmentRecoveryCase, UUID> {
    Optional<AppointmentRecoveryCase> findByAppointmentId(UUID appointmentId);
    List<AppointmentRecoveryCase> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from AppointmentRecoveryCase item where item.id = :id")
    Optional<AppointmentRecoveryCase> findByIdForUpdate(@Param("id") UUID id);
}
