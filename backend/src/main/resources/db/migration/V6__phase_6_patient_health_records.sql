ALTER TABLE doctors ADD COLUMN linked_user_id UUID UNIQUE REFERENCES users(id);

CREATE TABLE clinical_visits (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    recorded_by_user_id UUID NOT NULL REFERENCES users(id),
    visit_date DATE NOT NULL,
    symptoms VARCHAR(2000),
    diagnosis VARCHAR(2000) NOT NULL,
    doctor_notes VARCHAR(4000),
    discharge_summary VARCHAR(4000),
    follow_up_recommendation VARCHAR(2000),
    finalized_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    clinical_visit_id UUID NOT NULL UNIQUE REFERENCES clinical_visits(id),
    general_instructions VARCHAR(1200),
    prescribed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE prescription_items (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL REFERENCES prescriptions(id),
    item_order INTEGER NOT NULL,
    medicine_name VARCHAR(180) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(140) NOT NULL,
    duration_text VARCHAR(140) NOT NULL,
    route VARCHAR(80),
    instructions VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prescription_item_order UNIQUE (prescription_id, item_order),
    CONSTRAINT ck_prescription_item_order CHECK (item_order > 0)
);

CREATE TABLE patient_allergies (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    recorded_by_user_id UUID NOT NULL REFERENCES users(id),
    substance VARCHAR(180) NOT NULL,
    reaction VARCHAR(500),
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_patient_allergy_severity CHECK (severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_patient_allergy_status CHECK (status IN ('ACTIVE', 'RESOLVED'))
);

CREATE TABLE medical_documents (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(40) NOT NULL,
    original_filename VARCHAR(240) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(320) NOT NULL UNIQUE,
    sha256 VARCHAR(64) NOT NULL,
    document_date DATE NOT NULL,
    description VARCHAR(600),
    verification_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_medical_document_type CHECK (document_type IN (
        'LAB_REPORT', 'MRI_REPORT', 'CT_REPORT', 'X_RAY_REPORT', 'PRESCRIPTION',
        'DISCHARGE_SUMMARY', 'REFERRAL', 'OTHER'
    )),
    CONSTRAINT ck_document_verification CHECK (verification_status IN ('PATIENT_UPLOADED', 'CLINICIAN_VERIFIED')),
    CONSTRAINT ck_medical_document_size CHECK (size_bytes > 0 AND size_bytes <= 10485760)
);

CREATE INDEX idx_clinical_visits_patient_date ON clinical_visits(patient_id, visit_date DESC);
CREATE INDEX idx_clinical_visits_doctor ON clinical_visits(doctor_id, visit_date DESC);
CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id, item_order);
CREATE INDEX idx_patient_allergies_active ON patient_allergies(patient_id, status, severity);
CREATE INDEX idx_medical_documents_patient_date ON medical_documents(patient_id, document_date DESC);
CREATE INDEX idx_medical_documents_hospital ON medical_documents(hospital_id, document_type);
