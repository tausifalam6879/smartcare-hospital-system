CREATE TABLE doctor_day_operations (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    service_date DATE NOT NULL,
    status VARCHAR(40) NOT NULL,
    delay_minutes INTEGER NOT NULL,
    reason VARCHAR(300),
    updated_by_user_id UUID NOT NULL REFERENCES users(id),
    operational_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_doctor_day_operation UNIQUE (doctor_id, service_date),
    CONSTRAINT ck_doctor_day_status CHECK (status IN (
        'ON_TIME', 'DELAYED_30', 'DELAYED_60', 'EMERGENCY_INTERRUPTION',
        'TEMPORARILY_UNAVAILABLE', 'CANCELLED_FOR_DAY'
    )),
    CONSTRAINT ck_doctor_day_delay CHECK (delay_minutes IN (0, 30, 60))
);

CREATE INDEX idx_doctor_day_operation_dashboard ON doctor_day_operations(hospital_id, service_date, status);

CREATE TABLE appointment_recovery_cases (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    operation_id UUID NOT NULL REFERENCES doctor_day_operations(id),
    original_queue_position INTEGER,
    status VARCHAR(40) NOT NULL,
    patient_choice VARCHAR(40),
    target_doctor_id UUID REFERENCES doctors(id),
    target_date DATE,
    decided_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_recovery_status CHECK (status IN (
        'AWAITING_PATIENT_CHOICE', 'RESCHEDULED', 'REFUND_REVIEW_REQUIRED'
    )),
    CONSTRAINT ck_recovery_choice CHECK (patient_choice IS NULL OR patient_choice IN (
        'RESCHEDULE_SAME_DOCTOR', 'MOVE_TO_ELIGIBLE_DOCTOR', 'PRIORITY_FUTURE_QUEUE', 'REFUND_REVIEW'
    ))
);

CREATE INDEX idx_recovery_patient ON appointment_recovery_cases(patient_id, status, created_at DESC);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (type IN (
    'APPOINTMENT_RESERVED', 'PAYMENT_REQUIRED', 'PAYMENT_SUCCESSFUL', 'APPOINTMENT_CONFIRMED',
    'WAITLISTED', 'WAITLIST_PROMOTED', 'APPOINTMENT_CANCELLED', 'RESERVATION_EXPIRED',
    'CASH_PAYMENT_DEADLINE', 'CHECK_IN_CONFIRMED', 'QUEUE_POSITION_UPDATED', 'NOW_SERVING',
    'VISIT_COMPLETED', 'NO_SHOW', 'BLOOD_REQUEST_CREATED', 'BLOOD_REQUEST_UPDATED',
    'AMBULANCE_REQUEST_CREATED', 'AMBULANCE_REQUEST_UPDATED', 'DOCTOR_DELAYED',
    'DOCTOR_UNAVAILABLE', 'APPOINTMENT_RECOVERY_REQUIRED', 'APPOINTMENT_RESCHEDULED',
    'REFUND_REVIEW_REQUIRED'
));
