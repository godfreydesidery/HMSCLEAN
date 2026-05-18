package com.otapp.hmis.engine.masterdata.consumable.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ConsumableDtos {

    private ConsumableDtos() {}

    public record ConsumableDto(
            String uid, String code, String name,
            String unitOfMeasure, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateConsumableRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 32)  String unitOfMeasure,
            @Size(max = 500) String description) {}

    public record UpdateConsumableRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 32)  String unitOfMeasure,
            @Size(max = 500) String description) {}
}
