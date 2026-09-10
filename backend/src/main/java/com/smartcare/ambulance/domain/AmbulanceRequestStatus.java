package com.smartcare.ambulance.domain;

public enum AmbulanceRequestStatus {
    REQUESTED,
    ASSIGNED,
    ACKNOWLEDGED,
    EN_ROUTE_TO_PATIENT,
    PATIENT_PICKED_UP,
    EN_ROUTE_TO_HOSPITAL,
    ARRIVED,
    COMPLETED,
    CANCELLED
}
