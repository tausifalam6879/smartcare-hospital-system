package com.smartcare.patient.service;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;

@Service
public class PatientService implements PatientRegistration {

    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public String createProfile(UserAccount account, LocalDate dateOfBirth, String gender,
                                String emergencyContact, String address) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        Patient patient = repository.save(new Patient("RVQ-" + Year.now().getValue() + "-" + suffix,
                account, dateOfBirth, gender, emergencyContact, address));
        return patient.getPatientNumber();
    }
}
