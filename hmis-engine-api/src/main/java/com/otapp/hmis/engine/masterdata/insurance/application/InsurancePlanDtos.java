package com.otapp.hmis.engine.masterdata.insurance.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class InsurancePlanDtos {

    private InsurancePlanDtos() {}

    public record InsurancePlanDto(
            String uid, String code, String name,
            String providerUid, String providerName,
            boolean coversConsultation, boolean coversLab, boolean coversRadiology,
            boolean coversProcedure,    boolean coversMedicine, boolean coversAdmission,
            String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateInsurancePlanRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(min = 26, max = 26) String providerUid,
            boolean coversConsultation,
            boolean coversLab,
            boolean coversRadiology,
            boolean coversProcedure,
            boolean coversMedicine,
            boolean coversAdmission,
            @Size(max = 500) String description) {}

    public record UpdateInsurancePlanRequest(
            @NotBlank @Size(max = 200) String name,
            boolean coversConsultation,
            boolean coversLab,
            boolean coversRadiology,
            boolean coversProcedure,
            boolean coversMedicine,
            boolean coversAdmission,
            @Size(max = 500) String description) {}
}
