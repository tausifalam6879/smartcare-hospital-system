package com.smartcare.patient.service;

import com.smartcare.auth.domain.UserAccount;

import java.time.LocalDate;

public interface PatientRegistration {
    String createProfile(UserAccount account, LocalDate dateOfBirth, String gender, String emergencyContact, String address);
}
