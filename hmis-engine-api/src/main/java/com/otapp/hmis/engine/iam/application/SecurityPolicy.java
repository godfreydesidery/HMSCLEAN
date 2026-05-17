package com.otapp.hmis.engine.iam.application;

import java.time.Duration;

/**
 * Tunable account-security thresholds. Held here (rather than scattered
 * across services) so future migration to {@code @ConfigurationProperties}
 * is a single edit.
 */
public final class SecurityPolicy {

    private SecurityPolicy() {}

    /** After this many consecutive failed logins, the account is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** Duration an account stays locked after hitting the threshold. */
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    /** Minimum password length enforced on change / reset. */
    public static final int MIN_PASSWORD_LENGTH = 8;
}
