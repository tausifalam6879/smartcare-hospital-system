package com.raahmediq.checkin.repository;

import com.raahmediq.checkin.domain.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    Optional<CheckIn> findByAppointmentId(UUID appointmentId);
}
