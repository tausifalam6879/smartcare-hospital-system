CREATE TABLE blood_reaction_panels (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    submitted_by_user_id UUID NOT NULL REFERENCES users(id),
    anti_a_storage_key VARCHAR(300) NOT NULL UNIQUE,
    anti_b_storage_key VARCHAR(300) NOT NULL UNIQUE,
    anti_d_storage_key VARCHAR(300) NOT NULL UNIQUE,
    anti_a_filename VARCHAR(180) NOT NULL,
    anti_b_filename VARCHAR(180) NOT NULL,
    anti_d_filename VARCHAR(180) NOT NULL,
    anti_a_probability NUMERIC(7,6) NOT NULL,
    anti_b_probability NUMERIC(7,6) NOT NULL,
    anti_d_probability NUMERIC(7,6) NOT NULL,
    anti_a_confidence NUMERIC(7,6) NOT NULL,
    anti_b_confidence NUMERIC(7,6) NOT NULL,
    anti_d_confidence NUMERIC(7,6) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    suggested_group VARCHAR(30),
    status VARCHAR(50) NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_blood_reaction_panel_status CHECK (status IN ('PENDING_CLINICIAN_VERIFICATION', 'MANUAL_REVIEW_REQUIRED')),
    CONSTRAINT ck_blood_reaction_panel_probability CHECK (
        anti_a_probability >= 0 AND anti_a_probability <= 1 AND anti_b_probability >= 0 AND anti_b_probability <= 1
        AND anti_d_probability >= 0 AND anti_d_probability <= 1 AND anti_a_confidence >= 0 AND anti_a_confidence <= 1
        AND anti_b_confidence >= 0 AND anti_b_confidence <= 1 AND anti_d_confidence >= 0 AND anti_d_confidence <= 1
    )
);
CREATE INDEX idx_blood_reaction_panels_patient ON blood_reaction_panels(patient_id, created_at DESC);
