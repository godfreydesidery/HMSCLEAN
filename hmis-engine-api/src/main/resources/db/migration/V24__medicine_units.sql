-- ============================================================================
-- Medicine units (PROCESS.md §17.14 item 7 — conversion coefficients).
--
-- Each medicine has exactly one BASE unit (factor 1) plus zero-or-more
-- alternate units that define their size relative to the base via
-- factor_to_base — e.g. a medicine with base TAB can carry BLISTER
-- (factor 10) and BOX (factor 100). Stock balances and movements stay in
-- base units; conversion happens at the API boundary.
--
-- Phase 21 wires units into the pharmacy↔store and pharmacy↔pharmacy
-- transfer chains. Other stock paths (manual receive/adjust, dispense,
-- retail sale, GRN) keep their existing single-unit semantics for now.
-- ============================================================================

CREATE TABLE md_medicine_unit (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    medicine_uid    VARCHAR(26)  NOT NULL,
    code            VARCHAR(16)  NOT NULL,
    name            VARCHAR(80)  NOT NULL,
    factor_to_base  INTEGER      NOT NULL,
    base            BOOLEAN      NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_md_medicine_unit_uid           UNIQUE (uid),
    CONSTRAINT uk_md_medicine_unit_medicine_code UNIQUE (medicine_uid, code),
    CONSTRAINT ck_md_medicine_unit_factor        CHECK (factor_to_base > 0),
    CONSTRAINT ck_md_medicine_unit_base_factor   CHECK (NOT base OR factor_to_base = 1)
);

CREATE INDEX idx_md_medicine_unit_medicine ON md_medicine_unit(medicine_uid);
CREATE INDEX idx_md_medicine_unit_base     ON md_medicine_unit(medicine_uid, base);

-- One base unit per medicine — enforced via a partial unique index so a
-- medicine can only ever have one row with base=true.
CREATE UNIQUE INDEX uk_md_medicine_unit_one_base
    ON md_medicine_unit (medicine_uid)
    WHERE base;

-- Seed an EACH base unit for every existing medicine. Future units
-- created through the API get real ULIDs from Java; this only needs to
-- produce a unique 26-char string per row.
DO $$
DECLARE
    med_row RECORD;
BEGIN
    FOR med_row IN SELECT id, uid FROM md_medicine ORDER BY id LOOP
        INSERT INTO md_medicine_unit (
            uid, medicine_uid, code, name, factor_to_base, base, active,
            created_at, updated_at, created_by, updated_by, version)
        VALUES (
            '01MEDUNIT' || LPAD(med_row.id::text, 17, '0'),
            med_row.uid, 'EACH', 'Each', 1, TRUE, TRUE,
            NOW(), NOW(), 'system', 'system', 0);
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- Stamp the new unit_uid column on every transfer-line table. Nullable so
-- legacy rows (none exist yet, but the column is forward-compatible) read
-- as "base unit".
-- ----------------------------------------------------------------------------

ALTER TABLE pharmacy_to_store_ro_line   ADD COLUMN unit_uid VARCHAR(26);
ALTER TABLE store_to_pharmacy_to_line   ADD COLUMN unit_uid VARCHAR(26);
ALTER TABLE store_to_pharmacy_rn_line   ADD COLUMN unit_uid VARCHAR(26);

ALTER TABLE pharmacy_to_pharmacy_ro_line ADD COLUMN unit_uid VARCHAR(26);
ALTER TABLE pharmacy_to_pharmacy_to_line ADD COLUMN unit_uid VARCHAR(26);
ALTER TABLE pharmacy_to_pharmacy_rn_line ADD COLUMN unit_uid VARCHAR(26);
