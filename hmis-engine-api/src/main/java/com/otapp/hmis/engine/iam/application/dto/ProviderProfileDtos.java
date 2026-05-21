package com.otapp.hmis.engine.iam.application.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ProviderProfileDtos {

    private ProviderProfileDtos() {
    }

    public record UpsertProviderProfileRequest(
            @Size(max = 64) String specialty,
            @Size(max = 64) String registrationNo,
            @Size(max = 64) String licenseNo) {
    }

    public record ProviderProfileDto(
            String uid,
            String userUid,
            String specialty,
            String registrationNo,
            String licenseNo,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }
}
