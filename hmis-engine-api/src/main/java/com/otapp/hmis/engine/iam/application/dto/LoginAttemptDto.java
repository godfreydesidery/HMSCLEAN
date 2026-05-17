package com.otapp.hmis.engine.iam.application.dto;

import com.otapp.hmis.engine.iam.domain.LoginAttempt;
import java.time.Instant;

public record LoginAttemptDto(
        String uid,
        String username,
        LoginAttempt.Outcome outcome,
        String ipAddress,
        String userAgent,
        Instant attemptedAt) {}
