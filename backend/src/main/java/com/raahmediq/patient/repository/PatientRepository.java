package com.raahmediq.patient.repository;

import com.raahmediq.patient.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByUserId(UUID userId);
    Optional<Patient> findByPatientNumberIgnoreCase(String patientNumber);
}
