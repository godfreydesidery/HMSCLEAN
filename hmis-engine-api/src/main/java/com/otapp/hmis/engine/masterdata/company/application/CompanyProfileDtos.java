package com.otapp.hmis.engine.masterdata.company.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CompanyProfileDtos {

    private CompanyProfileDtos() {}

    public record CompanyProfileDto(
            String name,
            String shortName,
            String address,
            String city,
            String country,
            String phone,
            String email,
            String website,
            String taxId,
            String motto,
            String logoUrl,
            String currency,
            String timezone,
            Instant updatedAt,
            String updatedBy) {}

    public record UpdateCompanyProfileRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80)  String shortName,
            @Size(max = 255) String address,
            @Size(max = 80)  String city,
            @Size(max = 80)  String country,
            @Size(max = 32)  String phone,
            @Size(max = 120) String email,
            @Size(max = 120) String website,
            @Size(max = 64)  String taxId,
            @Size(max = 500) String motto,
            @Size(max = 500) String logoUrl,
            @Size(min = 3, max = 3) String currency,
            @Size(max = 64)  String timezone) {}
}
