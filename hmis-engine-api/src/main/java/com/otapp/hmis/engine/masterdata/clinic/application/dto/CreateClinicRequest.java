package com.otapp.hmis.engine.masterdata.clinic.application.dto;

import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClinicRequest(
        @NotBlank
        @Size(min = 2, max = 32)
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code may contain only uppercase letters, digits, underscore and hyphen")
        String code,

        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        ClinicType type,

        @Size(max = 500)
        String description,

        @Size(max = 80)
        String location) {
}
