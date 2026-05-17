package com.otapp.hmis.engine.iam.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Blocklist entry for a JWT we know should no longer be honoured. Keyed by
 * the token's {@code jti} (a UUID we mint at issue time). Rows can be
 * pruned once {@code expiresAt} is in the past — the token would fail
 * verification on its own at that point.
 */
@Entity
@Table(name = "iam_revoked_token",
       uniqueConstraints = @UniqueConstraint(name = "uk_revoked_token_jti", columnNames = "jti"),
       indexes = {
               @Index(name = "idx_revoked_token_username", columnList = "username"),
               @Index(name = "idx_revoked_token_expires",  columnList = "expires_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevokedToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String jti;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedToken(String jti, String username, Instant expiresAt) {
        this.jti = jti;
        this.username = username;
        this.revokedAt = Instant.now();
        this.expiresAt = expiresAt;
    }
}
