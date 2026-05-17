package com.otapp.hmis.engine.iam.application.dto;

public record UserSummary(
        String uid,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled) {
}
