-- ============================================================================
-- Phase 44: follow-up visit flag + consultation transfer between clinics
-- (PROCESS.md §17.2).
--
--   * follow_up_of_consultation_uid — when set, this visit is a follow-up
--     to the referenced prior consultation. Same patient (enforced in
--     service).
--   * transferred_to_consultation_uid / transferred_from_consultation_uid
--     plus transfer_reason + transferred_at — the original consultation
--     closes as TRANSFERRED (new status); a new BOOKED consultation is
--     created at the target clinic; both reference each other for audit.
-- ============================================================================

ALTER TABLE consultation
    ADD COLUMN follow_up_of_consultation_uid     VARCHAR(26),
    ADD COLUMN transferred_to_consultation_uid   VARCHAR(26),
    ADD COLUMN transferred_from_consultation_uid VARCHAR(26),
    ADD COLUMN transfer_reason                   VARCHAR(500),
    ADD COLUMN transferred_at                    TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_consultation_follow_up
    ON consultation(follow_up_of_consultation_uid)
    WHERE follow_up_of_consultation_uid IS NOT NULL;

CREATE INDEX idx_consultation_transferred_to
    ON consultation(transferred_to_consultation_uid)
    WHERE transferred_to_consultation_uid IS NOT NULL;
