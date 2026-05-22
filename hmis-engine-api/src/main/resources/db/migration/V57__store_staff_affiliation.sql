-- ============================================================================
-- Store keeper ⇄ Store affiliation
-- Restores the legacy StorePerson.stores many-to-many: a keeper works at one or
-- more stores, and store issue / transfer operations are restricted to a keeper
-- affiliated with the source store. Coupling to iam is by identity string
-- (canonical user_uid + denormalized username) — no cross-module FK.
-- ============================================================================

CREATE TABLE md_store_staff (
    id         BIGSERIAL PRIMARY KEY,
    uid        VARCHAR(26) NOT NULL,
    store_uid  VARCHAR(26) NOT NULL,
    user_uid   VARCHAR(26) NOT NULL,
    username   VARCHAR(64) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(80),
    updated_by VARCHAR(80),
    version    BIGINT,
    CONSTRAINT uk_md_store_staff_uid UNIQUE (uid),
    CONSTRAINT uk_md_store_staff     UNIQUE (store_uid, user_uid)
);

CREATE INDEX idx_md_store_staff_store ON md_store_staff(store_uid) WHERE active;
CREATE INDEX idx_md_store_staff_user  ON md_store_staff(user_uid);
