-- ============================================================================
-- Managed currency list (with a single system default) + negotiable price band
-- (min / max) on the service-price matrix.
-- ============================================================================

-- ----- Managed currencies --------------------------------------------------

CREATE TABLE md_currency (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26)  NOT NULL,
    code        VARCHAR(3)   NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    symbol      VARCHAR(8),
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_currency_uid  UNIQUE (uid),
    CONSTRAINT uk_md_currency_code UNIQUE (code)
);
-- At most one default currency, enforced at the DB level.
CREATE UNIQUE INDEX uk_md_currency_default ON md_currency (is_default) WHERE is_default = TRUE;

INSERT INTO md_currency (uid, code, name, symbol, is_default, active,
                         created_at, updated_at, created_by, updated_by, version) VALUES
    ('01JCUR00000000000000000TZS', 'TZS', 'Tanzanian Shilling', 'TSh', TRUE,  TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01JCUR00000000000000000USD', 'USD', 'US Dollar',          '$',   FALSE, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01JCUR00000000000000000KES', 'KES', 'Kenyan Shilling',    'KSh', FALSE, TRUE, NOW(), NOW(), 'system', 'system', 0),
    ('01JCUR00000000000000000UGX', 'UGX', 'Ugandan Shilling',   'USh', FALSE, TRUE, NOW(), NOW(), 'system', 'system', 0);

-- ----- Negotiable price band on the service-price matrix --------------------
-- min_amount / max_amount are optional; when set they bound both the standard
-- amount and any per-line override a biller negotiates (enforced in code).
ALTER TABLE md_service_price ADD COLUMN min_amount NUMERIC(14,2);
ALTER TABLE md_service_price ADD COLUMN max_amount NUMERIC(14,2);

-- ----- Currency is part of the price identity ------------------------------
-- A (payer, service) cell may now hold one row per currency — e.g. a clinic's
-- cash consultation priced in both TZS and USD. Widen the uniqueness from
-- (plan, kind, service) to (plan, kind, service, currency).
DROP INDEX uk_md_service_price;
CREATE UNIQUE INDEX uk_md_service_price
    ON md_service_price (COALESCE(plan_uid, ''), kind, service_uid, currency);
