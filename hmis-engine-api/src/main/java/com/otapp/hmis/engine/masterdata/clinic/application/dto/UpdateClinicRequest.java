package com.otapp.hmis.engine.masterdata.clinic.application.dto;

import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClinicRequest(
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
