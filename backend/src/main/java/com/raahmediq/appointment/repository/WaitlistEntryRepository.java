package com.raahmediq.appointment.repository;

import com.raahmediq.appointment.domain.WaitlistEntry;
import com.raahmediq.appointment.domain.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {
    Optional<WaitlistEntry> findByAppointmentId(UUID appointmentId);

    Optional<WaitlistEntry> findFirstByDoctorIdAndServiceDateAndStatusOrderBySequenceNumberAsc(
            UUID doctorId, LocalDate serviceDate, WaitlistStatus status);

    long countByDoctorIdAndServiceDateAndStatus(UUID doctorId, LocalDate serviceDate, WaitlistStatus status);

    long countByDoctorIdAndServiceDate(UUID doctorId, LocalDate serviceDate);
}
