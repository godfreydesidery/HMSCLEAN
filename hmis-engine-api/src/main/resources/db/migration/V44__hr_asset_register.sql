-- ============================================================================
-- Phase 42: HR asset register (PROCESS.md §12, §17.11).
--
-- Fixed-asset inventory — equipment, furniture, vehicles, etc. Separate
-- from consumable inventory (pharmacy / store) and from clinical
-- artifacts. Custodian linkage to iam.User is by username (loose link,
-- nullable). Category + location are free text for V1.
-- ============================================================================

CREATE TABLE hr_asset (
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(26)  NOT NULL,

    tag                 VARCHAR(64)  NOT NULL,
    name                VARCHAR(160) NOT NULL,
    category            VARCHAR(80),
    location            VARCHAR(120),
    description         VARCHAR(500),

    serial_no           VARCHAR(120),
    manufacturer        VARCHAR(120),
    model               VARCHAR(120),

    acquisition_date    DATE,
    acquisition_cost    NUMERIC(14,2),
    currency            VARCHAR(3),

    custodian_username  VARCHAR(64),

    status              VARCHAR(16)  NOT NULL,
    retired_at          DATE,
    retired_reason      VARCHAR(500),

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(80),
    updated_by          VARCHAR(80),
    version             BIGINT,
    CONSTRAINT uk_hr_asset_uid  UNIQUE (uid),
    CONSTRAINT uk_hr_asset_tag  UNIQUE (tag),
    CONSTRAINT ck_hr_asset_cost CHECK (acquisition_cost IS NULL OR acquisition_cost >= 0)
);

CREATE INDEX idx_hr_asset_status   ON hr_asset(status);
CREATE INDEX idx_hr_asset_category ON hr_asset(category);
CREATE INDEX idx_hr_asset_location ON hr_asset(location);
