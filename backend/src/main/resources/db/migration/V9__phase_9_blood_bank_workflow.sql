CREATE TABLE blood_banks (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(180) NOT NULL,
    address_line VARCHAR(300) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    distance_km NUMERIC(7, 2) NOT NULL,
    estimated_transfer_minutes INTEGER NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    authorized BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_blood_bank_code UNIQUE (hospital_id, code),
    CONSTRAINT ck_blood_bank_distance CHECK (distance_km >= 0),
    CONSTRAINT ck_blood_bank_transfer_time CHECK (estimated_transfer_minutes BETWEEN 0 AND 1440),
    CONSTRAINT ck_blood_bank_source CHECK (source_type IN (
        'HOSPITAL_MANAGED', 'AUTHORIZED_PARTNER', 'TRUSTED_INTEGRATION'
    ))
);

CREATE INDEX idx_blood_bank_hospital ON blood_banks(hospital_id, authorized, active, distance_km);

CREATE TABLE blood_inventory_batches (
    id UUID PRIMARY KEY,
    blood_bank_id UUID NOT NULL REFERENCES blood_banks(id),
    blood_group VARCHAR(20) NOT NULL,
    component VARCHAR(30) NOT NULL,
    batch_reference VARCHAR(80) NOT NULL,
    expires_on DATE NOT NULL,
    total_units INTEGER NOT NULL,
    reserved_units INTEGER NOT NULL,
    verification_status VARCHAR(20) NOT NULL,
    last_verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_blood_inventory_batch UNIQUE (blood_bank_id, batch_reference),
    CONSTRAINT ck_blood_inventory_group CHECK (blood_group IN (
        'A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE',
        'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'
    )),
    CONSTRAINT ck_blood_inventory_component CHECK (component IN (
        'WHOLE_BLOOD', 'PACKED_RED_CELLS', 'PLATELETS', 'FRESH_FROZEN_PLASMA'
    )),
    CONSTRAINT ck_blood_inventory_units CHECK (
        total_units >= 0 AND reserved_units >= 0 AND reserved_units <= total_units
    ),
    CONSTRAINT ck_blood_inventory_verification CHECK (verification_status IN ('VERIFIED', 'QUARANTINED'))
);

CREATE INDEX idx_blood_inventory_search ON blood_inventory_batches(
    blood_group, component, verification_status, expires_on, last_verified_at
);

CREATE TABLE blood_requests (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    appointment_id UUID REFERENCES appointments(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    blood_group VARCHAR(20) NOT NULL,
    component VARCHAR(30) NOT NULL,
    requested_units INTEGER NOT NULL,
    matched_units INTEGER NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    clinical_reason VARCHAR(2000) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_blood_request_idempotency UNIQUE (created_by_user_id, idempotency_key),
    CONSTRAINT ck_blood_request_group CHECK (blood_group IN (
        'A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE',
        'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'
    )),
    CONSTRAINT ck_blood_request_component CHECK (component IN (
        'WHOLE_BLOOD', 'PACKED_RED_CELLS', 'PLATELETS', 'FRESH_FROZEN_PLASMA'
    )),
    CONSTRAINT ck_blood_request_units CHECK (
        requested_units BETWEEN 1 AND 20 AND matched_units BETWEEN 0 AND requested_units
    ),
    CONSTRAINT ck_blood_request_urgency CHECK (urgency IN ('ROUTINE', 'URGENT', 'EMERGENCY')),
    CONSTRAINT ck_blood_request_status CHECK (status IN (
        'SEARCHING', 'PARTIALLY_RESERVED', 'RESERVED', 'UNAVAILABLE', 'FULFILLED', 'CANCELLED'
    ))
);

CREATE INDEX idx_blood_request_patient ON blood_requests(patient_id, created_at DESC);
CREATE INDEX idx_blood_request_worklist ON blood_requests(hospital_id, status, urgency, created_at);

CREATE TABLE blood_allocations (
    id UUID PRIMARY KEY,
    blood_request_id UUID NOT NULL REFERENCES blood_requests(id),
    inventory_batch_id UUID NOT NULL REFERENCES blood_inventory_batches(id),
    units INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    reserved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_blood_allocation_request_batch UNIQUE (blood_request_id, inventory_batch_id),
    CONSTRAINT ck_blood_allocation_units CHECK (units > 0),
    CONSTRAINT ck_blood_allocation_status CHECK (status IN ('RESERVED', 'FULFILLED', 'RELEASED'))
);

CREATE INDEX idx_blood_allocation_request ON blood_allocations(blood_request_id, status);

CREATE TABLE blood_donor_opt_ins (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    verified_blood_group VARCHAR(20),
    eligibility_status VARCHAR(30) NOT NULL,
    contact_preference VARCHAR(30) NOT NULL,
    consented_at TIMESTAMP WITH TIME ZONE NOT NULL,
    eligibility_verified_at TIMESTAMP WITH TIME ZONE,
    verified_by_user_id UUID REFERENCES users(id),
    withdrawn_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_blood_donor_group CHECK (verified_blood_group IS NULL OR verified_blood_group IN (
        'A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE',
        'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'
    )),
    CONSTRAINT ck_blood_donor_eligibility CHECK (eligibility_status IN (
        'PENDING_VERIFICATION', 'ELIGIBLE', 'TEMPORARILY_INELIGIBLE', 'WITHDRAWN'
    )),
    CONSTRAINT ck_blood_donor_contact CHECK (contact_preference IN ('MOBILE', 'SMS', 'WHATSAPP', 'EMAIL'))
);

CREATE INDEX idx_blood_donor_match ON blood_donor_opt_ins(verified_blood_group, eligibility_status);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (type IN (
    'APPOINTMENT_RESERVED', 'PAYMENT_REQUIRED', 'PAYMENT_SUCCESSFUL', 'APPOINTMENT_CONFIRMED',
    'WAITLISTED', 'WAITLIST_PROMOTED', 'APPOINTMENT_CANCELLED', 'RESERVATION_EXPIRED',
    'CASH_PAYMENT_DEADLINE', 'CHECK_IN_CONFIRMED', 'QUEUE_POSITION_UPDATED', 'NOW_SERVING',
    'VISIT_COMPLETED', 'NO_SHOW', 'BLOOD_REQUEST_CREATED', 'BLOOD_REQUEST_UPDATED'
));
