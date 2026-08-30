CREATE TABLE hospital_locations (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    code VARCHAR(40) NOT NULL,
    name_en VARCHAR(140) NOT NULL,
    name_hi VARCHAR(180) NOT NULL,
    type VARCHAR(30) NOT NULL,
    building VARCHAR(80) NOT NULL,
    floor_label VARCHAR(40) NOT NULL,
    zone VARCHAR(80),
    room_number VARCHAR(40),
    map_x INTEGER NOT NULL,
    map_y INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_hospital_location_code UNIQUE (hospital_id, code),
    CONSTRAINT ck_hospital_location_type CHECK (type IN (
        'ENTRANCE', 'REGISTRATION', 'LIFT', 'STAIRS', 'CORRIDOR', 'RECEPTION',
        'DOCTOR_ROOM', 'LAB', 'IMAGING', 'PHARMACY', 'EMERGENCY', 'EXIT'
    )),
    CONSTRAINT ck_hospital_location_map_x CHECK (map_x BETWEEN 0 AND 100),
    CONSTRAINT ck_hospital_location_map_y CHECK (map_y BETWEEN 0 AND 100)
);

CREATE TABLE navigation_paths (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    from_location_id UUID NOT NULL REFERENCES hospital_locations(id),
    to_location_id UUID NOT NULL REFERENCES hospital_locations(id),
    instruction_en VARCHAR(400) NOT NULL,
    instruction_hi VARCHAR(500) NOT NULL,
    reverse_instruction_en VARCHAR(400) NOT NULL,
    reverse_instruction_hi VARCHAR(500) NOT NULL,
    distance_meters INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    step_free BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_navigation_path_nodes UNIQUE (hospital_id, from_location_id, to_location_id),
    CONSTRAINT ck_navigation_path_distinct_nodes CHECK (from_location_id <> to_location_id),
    CONSTRAINT ck_navigation_path_distance CHECK (distance_meters > 0),
    CONSTRAINT ck_navigation_path_duration CHECK (duration_seconds > 0)
);

CREATE TABLE qr_checkpoints (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    location_id UUID NOT NULL UNIQUE REFERENCES hospital_locations(id),
    public_code VARCHAR(64) NOT NULL UNIQUE,
    label_en VARCHAR(160) NOT NULL,
    label_hi VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_hospital_location_code_ci ON hospital_locations(hospital_id, LOWER(code));
CREATE UNIQUE INDEX uk_qr_checkpoint_public_code_ci ON qr_checkpoints(LOWER(public_code));
CREATE INDEX idx_hospital_locations_map ON hospital_locations(hospital_id, floor_label, active);
CREATE INDEX idx_navigation_paths_hospital ON navigation_paths(hospital_id, active);
CREATE INDEX idx_navigation_paths_from ON navigation_paths(from_location_id);
CREATE INDEX idx_navigation_paths_to ON navigation_paths(to_location_id);
CREATE INDEX idx_qr_checkpoints_hospital ON qr_checkpoints(hospital_id, active);
