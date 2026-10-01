-- Legacy unassigned panels remain visible to their owner and SUPER_ADMIN only.
-- Do not guess a hospital from the patient's name or a staff browser selection.
ALTER TABLE blood_reaction_panels ADD COLUMN hospital_id UUID REFERENCES hospitals(id);
CREATE INDEX idx_reaction_panel_hospital ON blood_reaction_panels(hospital_id);
