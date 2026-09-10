package com.smartcare.patient.domain;

import com.smartcare.auth.domain.UserAccount;
import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient extends AuditableEntity {

    @Column(name = "patient_number", nullable = false, unique = true, length = 24)
    private String patientNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String gender;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Column(length = 500)
    private String address;

    protected Patient() {
    }

    public Patient(String patientNumber, UserAccount user, LocalDate dateOfBirth, String gender,
                   String emergencyContact, String address) {
        this.patientNumber = patientNumber;
        this.user = user;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.emergencyContact = emergencyContact;
        this.address = address;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public UserAccount getUser() {
        return user;
    }
}
