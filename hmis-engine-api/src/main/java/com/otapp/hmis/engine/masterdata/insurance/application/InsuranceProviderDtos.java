package com.otapp.hmis.engine.masterdata.insurance.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class InsuranceProviderDtos {

    private InsuranceProviderDtos() {}

    public record InsuranceProviderDto(
            String uid, String code, String name, String contactPerson, String phone,
            String email, String address, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateInsuranceProviderRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String contactPerson,
            @Size(max = 40)  String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 255) String address,
            @Size(max = 500) String description) {}

    public record UpdateInsuranceProviderRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String contactPerson,
            @Size(max = 40)  String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 255) String address,
            @Size(max = 500) String description) {}
}
