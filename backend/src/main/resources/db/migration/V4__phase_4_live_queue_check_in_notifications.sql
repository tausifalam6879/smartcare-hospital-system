ALTER TABLE appointments
    ADD COLUMN checked_in_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN consultation_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN no_show_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE appointments DROP CONSTRAINT ck_appointment_status;
ALTER TABLE appointments ADD CONSTRAINT ck_appointment_status CHECK (status IN (
    'WAITLISTED', 'RESERVED_PENDING_PAYMENT', 'CASH_PENDING', 'CONFIRMED',
    'CHECKED_IN', 'IN_CONSULTATION', 'COMPLETED', 'NO_SHOW', 'CANCELLED', 'EXPIRED'
));

CREATE TABLE check_ins (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    channel VARCHAR(30) NOT NULL,
    privacy_token VARCHAR(24) NOT NULL UNIQUE,
    checked_in_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_check_in_channel CHECK (channel IN ('MOBILE_WEB', 'QR_CODE', 'RECEPTION_DESK', 'KIOSK'))
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    appointment_id UUID REFERENCES appointments(id),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(140) NOT NULL,
    message VARCHAR(600) NOT NULL,
    deduplication_key VARCHAR(180) NOT NULL UNIQUE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_notification_type CHECK (type IN (
        'APPOINTMENT_RESERVED', 'PAYMENT_REQUIRED', 'PAYMENT_SUCCESSFUL', 'APPOINTMENT_CONFIRMED',
        'WAITLISTED', 'WAITLIST_PROMOTED', 'APPOINTMENT_CANCELLED', 'RESERVATION_EXPIRED',
        'CASH_PAYMENT_DEADLINE', 'CHECK_IN_CONFIRMED', 'QUEUE_POSITION_UPDATED', 'NOW_SERVING',
        'VISIT_COMPLETED', 'NO_SHOW'
    ))
);

CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id),
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    detail VARCHAR(300),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_notification_delivery_channel UNIQUE (notification_id, channel),
    CONSTRAINT ck_notification_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS', 'WHATSAPP', 'PUSH', 'VOICE_CALL')),
    CONSTRAINT ck_notification_delivery_status CHECK (status IN ('DELIVERED', 'FAILED', 'SKIPPED_NOT_CONFIGURED'))
);

CREATE INDEX idx_check_ins_appointment ON check_ins(appointment_id);
CREATE INDEX idx_appointments_live_queue ON appointments(doctor_id, service_date, status, queue_position);
CREATE INDEX idx_notifications_patient_created ON notifications(patient_id, created_at DESC);
CREATE INDEX idx_notifications_patient_unread ON notifications(patient_id, read_at) WHERE read_at IS NULL;
CREATE INDEX idx_notification_deliveries_status ON notification_deliveries(status, created_at);
