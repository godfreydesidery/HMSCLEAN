-- ============================================================================
-- Protected/system roles.
--
-- Adds an explicit, data-driven `protected` marker to iam_role so the
-- protected-identity guards (ProtectedIdentityPolicy) do not rely on a
-- hardcoded name list in Java. Back-fills protected = TRUE for the system
-- roles seeded by V2 (which drive worklist routing / privilege baselines).
-- Additive: existing rows default to FALSE.
-- ============================================================================

ALTER TABLE iam_role
    ADD COLUMN protected BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE iam_role
SET protected = TRUE
WHERE name IN (
    'ROOT',
    'ADMIN',
    'RECEPTION',
    'CLINICIAN',
    'NURSE',
    'PHARMACIST',
    'LABORATORIST',
    'RADIOGRAPHER',
    'CASHIER',
    'ACCOUNTANT',
    'HR',
    'PROCUREMENT',
    'STORE_PERSON',
    'MANAGEMENT'
);
