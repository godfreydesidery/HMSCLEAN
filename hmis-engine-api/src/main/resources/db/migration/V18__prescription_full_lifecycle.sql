-- ============================================================================
-- Expand the prescription state machine from the simplified
--   REQUESTED → DISPENSED / CANCELLED
-- to the legacy six-stage pharmacy workflow:
--   PENDING → ACCEPTED → HELD → VERIFIED → APPROVED → SOLD / REJECTED / CANCELLED.
--
-- Adds per-stage timestamps + a reject reason. Rewrites any pre-existing
-- value so the column matches the new enum.
-- ============================================================================

ALTER TABLE prescription
    ADD COLUMN accepted_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN held_at       TIMESTAMP WITH TIME ZONE,
    ADD COLUMN verified_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN rejected_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN reject_reason VARCHAR(255);

-- Map historical values onto the new enum. New prescriptions start as PENDING.
UPDATE prescription SET status = 'PENDING' WHERE status = 'REQUESTED';
UPDATE prescription SET status = 'SOLD'    WHERE status = 'DISPENSED';
