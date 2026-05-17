-- ============================================================================
-- IAM module schema: users, roles, privileges
--
-- Every aggregate root has an internal numeric `id` (used only inside the
-- service / persistence layer) and a public `uid` (ULID, 26 chars) that is
-- what gets exposed in URLs and DTOs.
-- ============================================================================

CREATE TABLE iam_privilege (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26) NOT NULL,
    name        VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_iam_privilege_name UNIQUE (name),
    CONSTRAINT uk_iam_privilege_uid  UNIQUE (uid)
);

CREATE TABLE iam_role (
    id          BIGSERIAL PRIMARY KEY,
    uid         VARCHAR(26) NOT NULL,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_iam_role_name UNIQUE (name),
    CONSTRAINT uk_iam_role_uid  UNIQUE (uid)
);

CREATE TABLE iam_role_privilege (
    role_id      BIGINT NOT NULL,
    privilege_id BIGINT NOT NULL,
    CONSTRAINT pk_iam_role_privilege PRIMARY KEY (role_id, privilege_id),
    CONSTRAINT uk_iam_role_privilege UNIQUE (role_id, privilege_id),
    CONSTRAINT fk_iam_role_privilege_role
        FOREIGN KEY (role_id) REFERENCES iam_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_iam_role_privilege_privilege
        FOREIGN KEY (privilege_id) REFERENCES iam_privilege(id) ON DELETE CASCADE
);

CREATE TABLE iam_user (
    id            BIGSERIAL PRIMARY KEY,
    uid           VARCHAR(26) NOT NULL,
    username      VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(80) NOT NULL,
    last_name     VARCHAR(80) NOT NULL,
    email         VARCHAR(120),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(80),
    updated_by    VARCHAR(80),
    version       BIGINT,
    CONSTRAINT uk_iam_user_username UNIQUE (username),
    CONSTRAINT uk_iam_user_uid      UNIQUE (uid)
);

CREATE TABLE iam_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_iam_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT uk_iam_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_iam_user_role_user
        FOREIGN KEY (user_id) REFERENCES iam_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_iam_user_role_role
        FOREIGN KEY (role_id) REFERENCES iam_role(id) ON DELETE CASCADE
);

CREATE INDEX idx_iam_user_enabled ON iam_user(enabled);
