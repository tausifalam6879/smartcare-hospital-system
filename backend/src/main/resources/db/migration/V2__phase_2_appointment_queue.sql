CREATE TABLE doctor_day_ledgers (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    service_date DATE NOT NULL,
    effective_capacity INTEGER NOT NULL,
    active_count INTEGER NOT NULL DEFAULT 0,
    next_position INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_doctor_day_ledger UNIQUE (doctor_id, service_date),
    CONSTRAINT ck_ledger_capacity CHECK (effective_capacity > 0),
    CONSTRAINT ck_ledger_active_count CHECK (active_count >= 0 AND active_count <= effective_capacity),
    CONSTRAINT ck_ledger_next_position CHECK (next_position > 0)
);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    service_date DATE NOT NULL,
    queue_position INTEGER,
    status VARCHAR(40) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    reservation_expires_at TIMESTAMP WITH TIME ZONE,
    cash_deadline_at TIMESTAMP WITH TIME ZONE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_appointment_queue_position UNIQUE (doctor_id, service_date, queue_position),
    CONSTRAINT uk_appointment_idempotency UNIQUE (patient_id, idempotency_key),
    CONSTRAINT ck_appointment_position CHECK (queue_position IS NULL OR queue_position > 0),
    CONSTRAINT ck_appointment_amount CHECK (amount >= 0),
    CONSTRAINT ck_appointment_status CHECK (status IN (
        'WAITLISTED', 'RESERVED_PENDING_PAYMENT', 'CASH_PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'
    )),
    CONSTRAINT ck_appointment_payment_method CHECK (payment_method IN ('ONLINE', 'CASH'))
);

CREATE TABLE waitlist_entries (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    service_date DATE NOT NULL,
    sequence_number BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    promoted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_waitlist_appointment UNIQUE (appointment_id),
    CONSTRAINT uk_waitlist_sequence UNIQUE (doctor_id, service_date, sequence_number),
    CONSTRAINT ck_waitlist_sequence CHECK (sequence_number > 0),
    CONSTRAINT ck_waitlist_status CHECK (status IN ('WAITING', 'PROMOTED', 'CANCELLED'))
);

CREATE INDEX idx_appointments_patient_date ON appointments(patient_id, service_date DESC, created_at DESC);
CREATE INDEX idx_appointments_doctor_date_status ON appointments(doctor_id, service_date, status);
CREATE INDEX idx_appointments_online_expiry ON appointments(reservation_expires_at)
    WHERE status = 'RESERVED_PENDING_PAYMENT';
CREATE INDEX idx_appointments_cash_expiry ON appointments(cash_deadline_at)
    WHERE status = 'CASH_PENDING';
CREATE INDEX idx_waitlist_fifo ON waitlist_entries(doctor_id, service_date, sequence_number)
    WHERE status = 'WAITING';
