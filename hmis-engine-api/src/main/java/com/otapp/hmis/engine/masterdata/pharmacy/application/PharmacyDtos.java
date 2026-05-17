package com.otapp.hmis.engine.masterdata.pharmacy.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PharmacyDtos {

    private PharmacyDtos() {}

    public record PharmacyDto(
            String uid, String code, String name, String location, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreatePharmacyRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9_-]+$") String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 80)  String location,
            @Size(max = 500) String description) {}

    public record UpdatePharmacyRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 80)  String location,
            @Size(max = 500) String description) {}
}
