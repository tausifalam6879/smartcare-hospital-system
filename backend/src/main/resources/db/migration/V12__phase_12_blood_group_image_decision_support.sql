CREATE TABLE blood_group_image_analyses (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    submitted_by_user_id UUID NOT NULL REFERENCES users(id),
    storage_key VARCHAR(300) NOT NULL UNIQUE,
    original_filename VARCHAR(180) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(40) NOT NULL,
    model_inference_status VARCHAR(40) NOT NULL,
    model_suggested_group VARCHAR(30),
    model_confidence NUMERIC(5,4),
    anti_a_reactive BOOLEAN,
    anti_b_reactive BOOLEAN,
    anti_d_reactive BOOLEAN,
    preliminary_group VARCHAR(30),
    observed_by_user_id UUID REFERENCES users(id),
    observation_note VARCHAR(500),
    observed_at TIMESTAMP WITH TIME ZONE,
    verified_group VARCHAR(30),
    verified_by_user_id UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    rejected_by_user_id UUID REFERENCES users(id),
    rejection_reason VARCHAR(300),
    rejected_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_blood_group_analysis_status CHECK (status IN (
        'SUBMITTED', 'OBSERVATIONS_RECORDED', 'LAB_VERIFIED', 'REJECTED'
    )),
    CONSTRAINT ck_blood_group_model_status CHECK (model_inference_status IN (
        'NOT_CONFIGURED', 'COMPLETED', 'FAILED'
    )),
    CONSTRAINT ck_blood_group_analysis_content CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_blood_group_analysis_size CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    CONSTRAINT ck_blood_group_model_confidence CHECK (
        model_confidence IS NULL OR (model_confidence >= 0 AND model_confidence <= 1)
    ),
    CONSTRAINT ck_blood_group_model_result CHECK (
        model_inference_status <> 'COMPLETED' OR (model_suggested_group IS NOT NULL AND model_confidence IS NOT NULL)
    ),
    CONSTRAINT ck_blood_group_observations CHECK (
        status = 'SUBMITTED' OR status = 'REJECTED' OR (
            anti_a_reactive IS NOT NULL AND anti_b_reactive IS NOT NULL AND anti_d_reactive IS NOT NULL
            AND preliminary_group IS NOT NULL AND observed_by_user_id IS NOT NULL AND observed_at IS NOT NULL
        )
    ),
    CONSTRAINT ck_blood_group_verification CHECK (
        status <> 'LAB_VERIFIED' OR (
            verified_group IS NOT NULL AND verified_by_user_id IS NOT NULL AND verified_at IS NOT NULL
            AND verified_by_user_id <> observed_by_user_id
        )
    ),
    CONSTRAINT ck_blood_group_rejection CHECK (
        status <> 'REJECTED' OR (
            rejection_reason IS NOT NULL AND rejected_by_user_id IS NOT NULL AND rejected_at IS NOT NULL
        )
    )
);

CREATE INDEX idx_blood_group_analysis_patient ON blood_group_image_analyses(patient_id, created_at DESC);
CREATE INDEX idx_blood_group_analysis_worklist ON blood_group_image_analyses(hospital_id, status, created_at);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (type IN (
    'APPOINTMENT_RESERVED', 'PAYMENT_REQUIRED', 'PAYMENT_SUCCESSFUL', 'APPOINTMENT_CONFIRMED',
    'WAITLISTED', 'WAITLIST_PROMOTED', 'APPOINTMENT_CANCELLED', 'RESERVATION_EXPIRED',
    'CASH_PAYMENT_DEADLINE', 'CHECK_IN_CONFIRMED', 'QUEUE_POSITION_UPDATED', 'NOW_SERVING',
    'VISIT_COMPLETED', 'NO_SHOW', 'BLOOD_REQUEST_CREATED', 'BLOOD_REQUEST_UPDATED',
    'AMBULANCE_REQUEST_CREATED', 'AMBULANCE_REQUEST_UPDATED', 'DOCTOR_DELAYED',
    'DOCTOR_UNAVAILABLE', 'APPOINTMENT_RECOVERY_REQUIRED', 'APPOINTMENT_RESCHEDULED',
    'REFUND_REVIEW_REQUIRED', 'BLOOD_GROUP_ANALYSIS_SUBMITTED', 'BLOOD_GROUP_ANALYSIS_UPDATED'
));
