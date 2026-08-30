package com.raahmediq.medicalrecord.repository;

import com.raahmediq.medicalrecord.domain.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, UUID> {
    List<PatientAllergy> findAllByPatientIdOrderByRecordedAtDesc(UUID patientId);
    List<PatientAllergy> findAllByAppointmentIdOrderByRecordedAtAsc(UUID appointmentId);
}
