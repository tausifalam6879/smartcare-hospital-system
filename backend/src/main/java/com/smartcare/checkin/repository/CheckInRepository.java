package com.smartcare.checkin.repository;

import com.smartcare.checkin.domain.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    Optional<CheckIn> findByAppointmentId(UUID appointmentId);
}
