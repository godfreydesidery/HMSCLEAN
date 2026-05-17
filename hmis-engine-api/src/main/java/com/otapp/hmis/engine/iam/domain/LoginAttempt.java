package com.otapp.hmis.engine.iam.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per authentication attempt — used to detect abuse, surface
 * suspicious patterns, and review who logged in when. Recorded even
 * when the username does not match any user (so admins can spot
 * targeted scans).
 */
@Entity
@Table(name = "iam_login_attempt",
       indexes = {
               @Index(name = "idx_login_attempt_username",   columnList = "username, attempted_at"),
               @Index(name = "idx_login_attempt_attempted",  columnList = "attempted_at"),
               @Index(name = "idx_login_attempt_outcome",    columnList = "outcome")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttempt extends AuditableEntity {

    public enum Outcome {
        SUCCESS,
        BAD_CREDENTIALS,
        USER_DISABLED,
        USER_LOCKED,
        UNKNOWN_USER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Outcome outcome;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    public LoginAttempt(String username, Outcome outcome, String ipAddress, String userAgent) {
        this.username = username;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.attemptedAt = Instant.now();
    }
}
