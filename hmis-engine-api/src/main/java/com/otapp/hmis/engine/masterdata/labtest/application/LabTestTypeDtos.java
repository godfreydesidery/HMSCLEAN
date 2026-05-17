package com.otapp.hmis.engine.masterdata.labtest.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class LabTestTypeDtos {

    private LabTestTypeDtos() {}

    public record LabTestTypeDto(
            String uid, String code, String name, String specimen, String unit, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateLabTestTypeRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 80)  String specimen,
            @Size(max = 32)  String unit,
            @Size(max = 500) String description) {}

    public record UpdateLabTestTypeRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 80)  String specimen,
            @Size(max = 32)  String unit,
            @Size(max = 500) String description) {}
}
