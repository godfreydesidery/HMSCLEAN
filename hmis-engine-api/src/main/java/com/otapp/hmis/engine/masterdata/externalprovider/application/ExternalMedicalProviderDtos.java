package com.otapp.hmis.engine.masterdata.externalprovider.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ExternalMedicalProviderDtos {

    private ExternalMedicalProviderDtos() {}

    public record ExternalMedicalProviderDto(
            String uid, String code, String name,
            String address, String telephone, String email, String fax, String website,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateExternalMedicalProviderRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 255) String address,
            @Size(max = 40)  String telephone,
            @Email @Size(max = 120) String email,
            @Size(max = 40)  String fax,
            @Size(max = 200) String website) {}

    public record UpdateExternalMedicalProviderRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 255) String address,
            @Size(max = 40)  String telephone,
            @Email @Size(max = 120) String email,
            @Size(max = 40)  String fax,
            @Size(max = 200) String website) {}
}
