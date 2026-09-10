CREATE TABLE care_follow_ups (
    id UUID PRIMARY KEY,
    clinical_visit_id UUID NOT NULL UNIQUE REFERENCES clinical_visits(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    follow_up_date DATE NOT NULL,
    instructions VARCHAR(2000),
    medication_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    patient_response_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_care_follow_up_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'MISSED'))
);

CREATE INDEX idx_care_follow_ups_patient_date ON care_follow_ups(patient_id, follow_up_date);
CREATE INDEX idx_care_follow_ups_doctor_status ON care_follow_ups(doctor_id, status, follow_up_date);
