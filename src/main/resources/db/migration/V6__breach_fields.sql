-- Breach register: when the breach was discovered (starts the PDPP 72-hour clock), what was actually
-- done to contain it, and who recorded it.
ALTER TABLE breach_incidents ADD COLUMN discovered_at datetime(6);
ALTER TABLE breach_incidents ADD COLUMN containment_actions TEXT;
ALTER TABLE breach_incidents ADD COLUMN reported_by varchar(255);
UPDATE breach_incidents SET discovered_at = detected_at WHERE discovered_at IS NULL;
