package com.smartcare.hospital.domain;

import com.smartcare.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "hospitals")
public class Hospital extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "address_line", nullable = false, length = 240)
    private String addressLine;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 12)
    private String postalCode;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "time_zone", nullable = false, length = 60)
    private String timeZone;

    @Column(nullable = false)
    private boolean active;

    protected Hospital() {
    }

    public Hospital(String code, String name, String addressLine, String city, String state,
                    String postalCode, String contactNumber, String timeZone) {
        apply(code, name, addressLine, city, state, postalCode, contactNumber, timeZone, true);
    }

    public void update(String code, String name, String addressLine, String city, String state,
                       String postalCode, String contactNumber, String timeZone, boolean active) {
        apply(code, name, addressLine, city, state, postalCode, contactNumber, timeZone, active);
    }

    private void apply(String code, String name, String addressLine, String city, String state,
                       String postalCode, String contactNumber, String timeZone, boolean active) {
        this.code = code;
        this.name = name;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.contactNumber = contactNumber;
        this.timeZone = timeZone;
        this.active = active;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getContactNumber() { return contactNumber; }
    public String getTimeZone() { return timeZone; }
    public boolean isActive() { return active; }
}
