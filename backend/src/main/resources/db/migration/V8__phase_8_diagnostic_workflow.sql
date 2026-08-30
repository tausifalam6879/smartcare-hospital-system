CREATE TABLE diagnostic_procedures (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(180) NOT NULL,
    modality VARCHAR(20) NOT NULL,
    preparation_instructions VARCHAR(1200),
    turnaround_hours INTEGER NOT NULL,
    daily_capacity INTEGER NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL,
    fee NUMERIC(12, 2) NOT NULL,
    building VARCHAR(80),
    floor_label VARCHAR(40),
    room_number VARCHAR(40),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_diagnostic_procedure_code UNIQUE (hospital_id, code),
    CONSTRAINT ck_diagnostic_procedure_modality CHECK (modality IN ('LAB', 'MRI', 'CT', 'X_RAY')),
    CONSTRAINT ck_diagnostic_procedure_turnaround CHECK (turnaround_hours BETWEEN 1 AND 720),
    CONSTRAINT ck_diagnostic_procedure_capacity CHECK (daily_capacity BETWEEN 1 AND 5000),
    CONSTRAINT ck_diagnostic_procedure_duration CHECK (estimated_duration_minutes BETWEEN 5 AND 480),
    CONSTRAINT ck_diagnostic_procedure_fee CHECK (fee >= 0)
);

CREATE INDEX idx_diagnostic_procedure_hospital_modality
    ON diagnostic_procedures(hospital_id, modality, active);

CREATE TABLE diagnostic_day_ledgers (
    id UUID PRIMARY KEY,
    procedure_id UUID NOT NULL REFERENCES diagnostic_procedures(id),
    service_date DATE NOT NULL,
    effective_capacity INTEGER NOT NULL,
    active_count INTEGER NOT NULL,
    next_position INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_diagnostic_day_ledger UNIQUE (procedure_id, service_date),
    CONSTRAINT ck_diagnostic_ledger_capacity CHECK (effective_capacity > 0),
    CONSTRAINT ck_diagnostic_ledger_active CHECK (active_count BETWEEN 0 AND effective_capacity),
    CONSTRAINT ck_diagnostic_ledger_position CHECK (next_position > 0)
);

CREATE TABLE diagnostic_orders (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    ordered_by_doctor_id UUID NOT NULL REFERENCES doctors(id),
    procedure_id UUID NOT NULL REFERENCES diagnostic_procedures(id),
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    clinical_note VARCHAR(2000),
    scheduled_date DATE,
    queue_position INTEGER,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    sample_collected_at TIMESTAMP WITH TIME ZONE,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    result_verified_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_diagnostic_order_appointment_procedure UNIQUE (appointment_id, procedure_id),
    CONSTRAINT uk_diagnostic_order_queue UNIQUE (procedure_id, scheduled_date, queue_position),
    CONSTRAINT ck_diagnostic_order_status CHECK (status IN (
        'ORDERED', 'SCHEDULED', 'SAMPLE_COLLECTED', 'IN_PROGRESS', 'RESULT_VERIFIED', 'CANCELLED'
    )),
    CONSTRAINT ck_diagnostic_order_priority CHECK (priority IN ('ROUTINE', 'URGENT')),
    CONSTRAINT ck_diagnostic_order_schedule_fields CHECK (
        (scheduled_date IS NULL AND queue_position IS NULL) OR
        (scheduled_date IS NOT NULL AND queue_position IS NOT NULL AND queue_position > 0)
    )
);

CREATE INDEX idx_diagnostic_order_patient ON diagnostic_orders(patient_id, created_at DESC);
CREATE INDEX idx_diagnostic_order_worklist
    ON diagnostic_orders(procedure_id, scheduled_date, status, queue_position);

CREATE TABLE diagnostic_results (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES diagnostic_orders(id),
    summary VARCHAR(2000) NOT NULL,
    findings VARCHAR(6000),
    impression VARCHAR(3000),
    overall_flag VARCHAR(20) NOT NULL,
    verified_by_user_id UUID NOT NULL REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_diagnostic_result_flag CHECK (overall_flag IN ('NORMAL', 'ABNORMAL', 'CRITICAL', 'INDETERMINATE'))
);

CREATE TABLE diagnostic_result_items (
    id UUID PRIMARY KEY,
    result_id UUID NOT NULL REFERENCES diagnostic_results(id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL,
    name VARCHAR(180) NOT NULL,
    result_value VARCHAR(180) NOT NULL,
    unit VARCHAR(80),
    reference_range VARCHAR(180),
    flag VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_diagnostic_result_item_order UNIQUE (result_id, item_order),
    CONSTRAINT ck_diagnostic_result_item_flag CHECK (flag IN ('NORMAL', 'ABNORMAL', 'CRITICAL', 'INDETERMINATE'))
);

CREATE INDEX idx_diagnostic_result_items_result ON diagnostic_result_items(result_id, item_order);
