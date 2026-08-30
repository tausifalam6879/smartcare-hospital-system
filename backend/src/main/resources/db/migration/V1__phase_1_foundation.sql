CREATE TABLE users (
    id UUID PRIMARY KEY,
    mobile_number VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(254),
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    preferred_language VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN (
        'PATIENT', 'DOCTOR', 'RECEPTIONIST', 'CASHIER', 'LAB_TECHNICIAN',
        'BLOOD_BANK_STAFF', 'AMBULANCE_DISPATCHER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'
    ))
);

CREATE TABLE patients (
    id UUID PRIMARY KEY,
    patient_number VARCHAR(24) NOT NULL UNIQUE,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    date_of_birth DATE,
    gender VARCHAR(30),
    emergency_contact VARCHAR(20),
    address VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE hospitals (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    address_line VARCHAR(240) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(12) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    time_zone VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_department_hospital_code UNIQUE (hospital_id, code)
);

CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    department_id UUID NOT NULL REFERENCES departments(id),
    name VARCHAR(140) NOT NULL,
    specialization VARCHAR(140) NOT NULL,
    registration_number VARCHAR(60) NOT NULL UNIQUE,
    consultation_fee NUMERIC(12, 2) NOT NULL,
    expected_consultation_minutes INTEGER NOT NULL,
    daily_max_capacity INTEGER NOT NULL,
    building VARCHAR(80),
    floor_label VARCHAR(40),
    room_number VARCHAR(40),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_doctor_fee CHECK (consultation_fee >= 0),
    CONSTRAINT ck_doctor_duration CHECK (expected_consultation_minutes BETWEEN 5 AND 240),
    CONSTRAINT ck_doctor_capacity CHECK (daily_max_capacity BETWEEN 1 AND 1000)
);

CREATE TABLE doctor_schedules (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    capacity_override INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_doctor_schedule_start UNIQUE (doctor_id, day_of_week, start_time),
    CONSTRAINT ck_schedule_time CHECK (start_time < end_time),
    CONSTRAINT ck_schedule_duration CHECK (slot_duration_minutes BETWEEN 5 AND 240),
    CONSTRAINT ck_schedule_capacity CHECK (capacity_override IS NULL OR capacity_override > 0),
    CONSTRAINT ck_schedule_day CHECK (day_of_week IN (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
    ))
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_subject VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id UUID,
    outcome VARCHAR(30) NOT NULL,
    hospital_id UUID,
    correlation_id VARCHAR(100),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_departments_hospital ON departments(hospital_id);
CREATE INDEX idx_doctors_hospital_department ON doctors(hospital_id, department_id) WHERE active = TRUE;
CREATE INDEX idx_doctors_name ON doctors(name);
CREATE INDEX idx_doctor_schedules_doctor_day ON doctor_schedules(doctor_id, day_of_week);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id, occurred_at DESC);
CREATE INDEX idx_audit_actor ON audit_logs(actor_subject, occurred_at DESC);
CREATE UNIQUE INDEX uk_users_email_ci ON users(LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_hospitals_code_ci ON hospitals(LOWER(code));
CREATE UNIQUE INDEX uk_departments_hospital_code_ci ON departments(hospital_id, LOWER(code));
CREATE UNIQUE INDEX uk_doctors_registration_ci ON doctors(LOWER(registration_number));
