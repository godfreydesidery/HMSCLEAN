-- ============================================================================
-- Phase 33: wire the Phase 28 picklist masterdata (Dosage,
-- AdministrationRoute, DosingFrequency) into Prescription. The existing
-- free-text dose / frequency columns stay — when a uid is given the
-- service denormalises the picklist's name into the free-text column so
-- read paths that only know about dose / frequency continue to work.
-- The new {@code route} field is additive (was missing entirely).
-- ============================================================================

ALTER TABLE prescription ADD COLUMN dosage_uid    VARCHAR(26);
ALTER TABLE prescription ADD COLUMN route_uid     VARCHAR(26);
ALTER TABLE prescription ADD COLUMN route         VARCHAR(80);
ALTER TABLE prescription ADD COLUMN frequency_uid VARCHAR(26);
