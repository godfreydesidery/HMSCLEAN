package com.otapp.hmis.engine.masterdata.procedure.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ProcedureTypeDtos {

    private ProcedureTypeDtos() {}

    public record ProcedureTypeDto(
            String uid, String code, String name, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateProcedureTypeRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String description) {}

    public record UpdateProcedureTypeRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String description) {}
}
