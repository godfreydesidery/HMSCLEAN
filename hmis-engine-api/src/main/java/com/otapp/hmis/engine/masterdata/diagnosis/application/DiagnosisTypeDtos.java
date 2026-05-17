package com.otapp.hmis.engine.masterdata.diagnosis.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class DiagnosisTypeDtos {

    private DiagnosisTypeDtos() {}

    public record DiagnosisTypeDto(
            String uid, String code, String name, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateDiagnosisTypeRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String description) {}

    public record UpdateDiagnosisTypeRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String description) {}
}
