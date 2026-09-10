ALTER TABLE blood_reaction_panels DROP CONSTRAINT ck_blood_reaction_panel_status;
ALTER TABLE blood_reaction_panels ADD COLUMN reviewed_by_user_id UUID REFERENCES users(id);
ALTER TABLE blood_reaction_panels ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE blood_reaction_panels ADD COLUMN review_note VARCHAR(500);
ALTER TABLE blood_reaction_panels ADD COLUMN rejection_reason VARCHAR(500);
ALTER TABLE blood_reaction_panels ADD CONSTRAINT ck_blood_reaction_panel_status CHECK (status IN ('PENDING_CLINICIAN_VERIFICATION', 'MANUAL_REVIEW_REQUIRED', 'CLINICIAN_VERIFIED', 'REJECTED'));
