CREATE TABLE staff_invitations (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    mobile_number VARCHAR(20) NOT NULL,
    account_type VARCHAR(40) NOT NULL,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    doctor_id UUID REFERENCES doctors(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
