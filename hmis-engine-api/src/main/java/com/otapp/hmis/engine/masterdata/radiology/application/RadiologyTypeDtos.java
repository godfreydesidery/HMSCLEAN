package com.otapp.hmis.engine.masterdata.radiology.application;

import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyModality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class RadiologyTypeDtos {

    private RadiologyTypeDtos() {}

    public record RadiologyTypeDto(
            String uid, String code, String name, RadiologyModality modality, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateRadiologyTypeRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @NotNull  RadiologyModality modality,
            @Size(max = 500) String description) {}

    public record UpdateRadiologyTypeRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull  RadiologyModality modality,
            @Size(max = 500) String description) {}
}
