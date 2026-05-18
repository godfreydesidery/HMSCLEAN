package com.otapp.hmis.engine.masterdata.administrationroute.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AdministrationRouteDtos {

    private AdministrationRouteDtos() {}

    public record AdministrationRouteDto(String uid, String code, String name, String description,
                                         boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateAdministrationRouteRequest(
            @NotBlank @Size(min = 1, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description) {}

    public record UpdateAdministrationRouteRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description) {}
}
