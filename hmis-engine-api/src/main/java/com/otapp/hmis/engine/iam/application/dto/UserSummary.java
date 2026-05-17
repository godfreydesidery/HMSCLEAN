package com.otapp.hmis.engine.iam.application.dto;

import java.time.Instant;
import java.util.List;

public record UserSummary(
        String uid,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean locked,
        boolean passwordMustChange,
        Instant lockedUntil,
        Instant lastLoginAt,
        List<String> roles) {
}
