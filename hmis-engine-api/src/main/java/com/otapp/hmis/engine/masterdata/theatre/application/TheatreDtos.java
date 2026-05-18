package com.otapp.hmis.engine.masterdata.theatre.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TheatreDtos {

    private TheatreDtos() {}

    public record TheatreDto(
            String uid,
            String code,
            String name,
            String location,
            String description,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateTheatreRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 80)  String location,
            @Size(max = 500) String description) {}

    public record UpdateTheatreRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 80)  String location,
            @Size(max = 500) String description) {}
}
