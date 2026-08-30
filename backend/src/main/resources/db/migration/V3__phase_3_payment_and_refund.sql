CREATE TABLE payments (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    provider_reference VARCHAR(120) NOT NULL,
    provider_transaction_id VARCHAR(120),
    receipt_number VARCHAR(40),
    expires_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_appointment UNIQUE (appointment_id),
    CONSTRAINT uk_payment_idempotency UNIQUE (patient_id, idempotency_key),
    CONSTRAINT uk_payment_provider_transaction UNIQUE (provider, provider_transaction_id),
    CONSTRAINT ck_payment_method CHECK (payment_method IN ('ONLINE', 'CASH')),
    CONSTRAINT ck_payment_status CHECK (status IN (
        'PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED'
    )),
    CONSTRAINT ck_payment_provider CHECK (provider IN ('DEVELOPMENT')),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0),
    CONSTRAINT ck_payment_currency CHECK (currency = 'INR')
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id),
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(300) NOT NULL,
    provider_reference VARCHAR(120) NOT NULL,
    provider_refund_id VARCHAR(120),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_refund_amount CHECK (amount >= 0),
    CONSTRAINT ck_refund_status CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED'))
);

CREATE TABLE payment_webhook_events (
    id UUID PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(120) NOT NULL,
    payment_id UUID NOT NULL REFERENCES payments(id),
    event_type VARCHAR(30) NOT NULL,
    payload_sha256 VARCHAR(64) NOT NULL,
    outcome VARCHAR(40) NOT NULL,
    provider_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_webhook_event UNIQUE (provider, provider_event_id),
    CONSTRAINT ck_webhook_provider CHECK (provider IN ('DEVELOPMENT')),
    CONSTRAINT ck_webhook_type CHECK (event_type IN ('SUCCEEDED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX idx_payments_patient_created ON payments(patient_id, created_at DESC);
CREATE INDEX idx_payments_status_expiry ON payments(status, expires_at);
CREATE INDEX idx_refunds_status_created ON refunds(status, created_at);
CREATE INDEX idx_payment_webhook_payment ON payment_webhook_events(payment_id, created_at DESC);
