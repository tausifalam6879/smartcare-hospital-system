CREATE TABLE staff_hospitals (
    user_id UUID NOT NULL REFERENCES users(id),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    PRIMARY KEY (user_id, hospital_id)
);
CREATE INDEX idx_staff_hospitals_hospital ON staff_hospitals(hospital_id);
-- Existing accepted invitations are authoritative assignments, unlike browser selections.
INSERT INTO staff_hospitals(user_id, hospital_id)
SELECT DISTINCT u.id, i.hospital_id FROM users u
JOIN staff_invitations i ON i.mobile_number = u.mobile_number
WHERE i.consumed_at IS NOT NULL;
