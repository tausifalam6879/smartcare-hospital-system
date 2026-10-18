CREATE TABLE staff_access_requests (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    email VARCHAR(254),
    account_type VARCHAR(40) NOT NULL,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    message VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invitation_id UUID REFERENCES staff_invitations(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_staff_access_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX ix_staff_access_request_inbox ON staff_access_requests(hospital_id, status, created_at);
CREATE UNIQUE INDEX uq_staff_access_request_pending
    ON staff_access_requests(hospital_id, mobile_number, account_type) WHERE status = 'PENDING';
CREATE INDEX ix_staff_invitation_hospital_created ON staff_invitations(hospital_id, created_at);
