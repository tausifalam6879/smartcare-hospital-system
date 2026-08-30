package com.raahmediq.patient.service;

import com.raahmediq.auth.domain.UserAccount;

import java.time.LocalDate;

public interface PatientRegistration {
    String createProfile(UserAccount account, LocalDate dateOfBirth, String gender, String emergencyContact, String address);
}
