package com.otapp.hmis.engine.masterdata.dosingfrequency.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class DosingFrequencyDtos {

    private DosingFrequencyDtos() {}

    public record DosingFrequencyDto(
            String uid, String code, String name,
            Integer timesPerDay, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateDosingFrequencyRequest(
            @NotBlank @Size(min = 1, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 120) String name,
            @Min(0) Integer timesPerDay,
            @Size(max = 500) String description) {}

    public record UpdateDosingFrequencyRequest(
            @NotBlank @Size(max = 120) String name,
            @Min(0) Integer timesPerDay,
            @Size(max = 500) String description) {}
}
