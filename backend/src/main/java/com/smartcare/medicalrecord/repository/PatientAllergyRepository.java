package com.smartcare.medicalrecord.repository;

import com.smartcare.medicalrecord.domain.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, UUID> {
    List<PatientAllergy> findAllByPatientIdOrderByRecordedAtDesc(UUID patientId);
    List<PatientAllergy> findAllByAppointmentIdOrderByRecordedAtAsc(UUID appointmentId);
}
