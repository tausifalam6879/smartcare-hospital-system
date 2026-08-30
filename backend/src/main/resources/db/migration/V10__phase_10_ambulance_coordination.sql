CREATE TABLE ambulances (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    registration_number VARCHAR(30) NOT NULL,
    call_sign VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    crew_label VARCHAR(120) NOT NULL,
    crew_contact VARCHAR(20),
    current_area VARCHAR(180),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    location_updated_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL,
    synthetic BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ambulance_registration UNIQUE (registration_number),
    CONSTRAINT uk_ambulance_call_sign UNIQUE (hospital_id, call_sign),
    CONSTRAINT ck_ambulance_status CHECK (status IN (
        'AVAILABLE', 'ASSIGNED', 'EN_ROUTE_TO_PATIENT', 'PATIENT_PICKED_UP',
        'EN_ROUTE_TO_HOSPITAL', 'ARRIVED', 'OUT_OF_SERVICE'
    )),
    CONSTRAINT ck_ambulance_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_ambulance_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_ambulance_availability ON ambulances(hospital_id, active, status, call_sign);

CREATE TABLE ambulance_requests (
    id UUID PRIMARY KEY,
    patient_id UUID REFERENCES patients(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    requested_by_user_id UUID NOT NULL REFERENCES users(id),
    ambulance_id UUID REFERENCES ambulances(id),
    transport_type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    pickup_address VARCHAR(500) NOT NULL,
    pickup_landmark VARCHAR(180),
    contact_number VARCHAR(20) NOT NULL,
    assistance_notes VARCHAR(500),
    idempotency_key VARCHAR(100) NOT NULL,
    status_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    dispatched_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ambulance_request_idempotency UNIQUE (requested_by_user_id, idempotency_key),
    CONSTRAINT ck_ambulance_request_transport CHECK (transport_type IN ('PATIENT_TRANSPORT', 'BLOOD_TRANSPORT')),
    CONSTRAINT ck_ambulance_request_priority CHECK (priority IN ('EMERGENCY', 'URGENT', 'SCHEDULED')),
    CONSTRAINT ck_ambulance_request_status CHECK (status IN (
        'REQUESTED', 'ASSIGNED', 'ACKNOWLEDGED', 'EN_ROUTE_TO_PATIENT', 'PATIENT_PICKED_UP',
        'EN_ROUTE_TO_HOSPITAL', 'ARRIVED', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_ambulance_request_patient ON ambulance_requests(patient_id, created_at DESC);
CREATE INDEX idx_ambulance_request_worklist ON ambulance_requests(hospital_id, status, priority, created_at);
CREATE INDEX idx_ambulance_request_vehicle ON ambulance_requests(ambulance_id, status);

CREATE TABLE ambulance_request_events (
    id UUID PRIMARY KEY,
    ambulance_request_id UUID NOT NULL REFERENCES ambulance_requests(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    note VARCHAR(300),
    event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ambulance_event_from_status CHECK (from_status IS NULL OR from_status IN (
        'REQUESTED', 'ASSIGNED', 'ACKNOWLEDGED', 'EN_ROUTE_TO_PATIENT', 'PATIENT_PICKED_UP',
        'EN_ROUTE_TO_HOSPITAL', 'ARRIVED', 'COMPLETED', 'CANCELLED'
    )),
    CONSTRAINT ck_ambulance_event_to_status CHECK (to_status IN (
        'REQUESTED', 'ASSIGNED', 'ACKNOWLEDGED', 'EN_ROUTE_TO_PATIENT', 'PATIENT_PICKED_UP',
        'EN_ROUTE_TO_HOSPITAL', 'ARRIVED', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_ambulance_event_timeline ON ambulance_request_events(ambulance_request_id, event_at, created_at);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (type IN (
    'APPOINTMENT_RESERVED', 'PAYMENT_REQUIRED', 'PAYMENT_SUCCESSFUL', 'APPOINTMENT_CONFIRMED',
    'WAITLISTED', 'WAITLIST_PROMOTED', 'APPOINTMENT_CANCELLED', 'RESERVATION_EXPIRED',
    'CASH_PAYMENT_DEADLINE', 'CHECK_IN_CONFIRMED', 'QUEUE_POSITION_UPDATED', 'NOW_SERVING',
    'VISIT_COMPLETED', 'NO_SHOW', 'BLOOD_REQUEST_CREATED', 'BLOOD_REQUEST_UPDATED',
    'AMBULANCE_REQUEST_CREATED', 'AMBULANCE_REQUEST_UPDATED'
));
