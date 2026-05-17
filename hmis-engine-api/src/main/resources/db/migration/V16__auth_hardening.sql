-- ============================================================================
-- Auth hardening: account lockout, forced password change, login audit.
-- ============================================================================

ALTER TABLE iam_user
    ADD COLUMN password_must_change   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN password_changed_at    TIMESTAMP WITH TIME ZONE,
    ADD COLUMN failed_login_attempts  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until           TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_login_at          TIMESTAMP WITH TIME ZONE;

CREATE TABLE iam_login_attempt (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    username        VARCHAR(64)  NOT NULL,
    outcome         VARCHAR(24)  NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    attempted_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_iam_login_attempt_uid UNIQUE (uid)
);

CREATE INDEX idx_login_attempt_username  ON iam_login_attempt(username, attempted_at);
CREATE INDEX idx_login_attempt_attempted ON iam_login_attempt(attempted_at);
CREATE INDEX idx_login_attempt_outcome   ON iam_login_attempt(outcome);

CREATE TABLE iam_revoked_token (
    id              BIGSERIAL PRIMARY KEY,
    uid             VARCHAR(26)  NOT NULL,

    jti             VARCHAR(64)  NOT NULL,
    username        VARCHAR(64)  NOT NULL,
    revoked_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(80),
    updated_by      VARCHAR(80),
    version         BIGINT,
    CONSTRAINT uk_iam_revoked_token_uid UNIQUE (uid),
    CONSTRAINT uk_iam_revoked_token_jti UNIQUE (jti)
);

CREATE INDEX idx_revoked_token_username ON iam_revoked_token(username);
CREATE INDEX idx_revoked_token_expires  ON iam_revoked_token(expires_at);
