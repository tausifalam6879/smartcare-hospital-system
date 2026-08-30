CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    title VARCHAR(120) NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    safety_class VARCHAR(40) NOT NULL,
    grounded BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ai_message_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_ai_message_safety CHECK (safety_class IN (
        'NORMAL', 'EMERGENCY_ESCALATION', 'CLINICAL_BOUNDARY', 'EVIDENCE_UNAVAILABLE'
    ))
);

CREATE TABLE ai_knowledge_index_states (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    source_type VARCHAR(30) NOT NULL,
    source_key UUID NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_code VARCHAR(80),
    indexed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ai_index_source UNIQUE (patient_id, source_type, source_key),
    CONSTRAINT ck_ai_index_source_type CHECK (source_type IN ('CLINICAL_VISIT', 'MEDICAL_DOCUMENT')),
    CONSTRAINT ck_ai_index_status CHECK (status IN ('INDEXED', 'NO_TEXT', 'OCR_REQUIRED', 'FAILED'))
);

CREATE TABLE document_chunk_metadata (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    hospital_id UUID REFERENCES hospitals(id),
    source_type VARCHAR(30) NOT NULL,
    source_key UUID NOT NULL,
    medical_document_id UUID REFERENCES medical_documents(id),
    clinical_visit_id UUID REFERENCES clinical_visits(id),
    chunk_index INTEGER NOT NULL,
    page_number INTEGER,
    citation_label VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    embedding_model VARCHAR(60) NOT NULL,
    embedding TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    extracted_text BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_document_chunk_source UNIQUE (patient_id, source_type, source_key, chunk_index),
    CONSTRAINT ck_document_chunk_source_type CHECK (source_type IN ('CLINICAL_VISIT', 'MEDICAL_DOCUMENT')),
    CONSTRAINT ck_document_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_document_chunk_page CHECK (page_number IS NULL OR page_number > 0),
    CONSTRAINT ck_document_chunk_reference CHECK (
        (source_type = 'CLINICAL_VISIT' AND clinical_visit_id IS NOT NULL AND medical_document_id IS NULL)
        OR
        (source_type = 'MEDICAL_DOCUMENT' AND medical_document_id IS NOT NULL AND clinical_visit_id IS NULL)
    )
);

CREATE TABLE ai_message_citations (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
    chunk_id UUID NOT NULL REFERENCES document_chunk_metadata(id),
    citation_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ai_message_citation_order UNIQUE (message_id, citation_order),
    CONSTRAINT uk_ai_message_citation_chunk UNIQUE (message_id, chunk_id),
    CONSTRAINT ck_ai_message_citation_order CHECK (citation_order > 0)
);

CREATE INDEX idx_ai_conversations_patient_updated ON ai_conversations(patient_id, last_activity_at DESC);
CREATE INDEX idx_ai_messages_conversation_created ON ai_messages(conversation_id, created_at);
CREATE INDEX idx_ai_index_states_patient ON ai_knowledge_index_states(patient_id, source_type, status);
CREATE INDEX idx_document_chunks_patient ON document_chunk_metadata(patient_id, source_type, created_at DESC);
CREATE INDEX idx_document_chunks_document ON document_chunk_metadata(medical_document_id, page_number, chunk_index);
CREATE INDEX idx_ai_citations_message ON ai_message_citations(message_id, citation_order);
