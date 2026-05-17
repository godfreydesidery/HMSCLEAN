package com.otapp.hmis.engine.masterdata.ward.application.dto;

import com.otapp.hmis.engine.masterdata.ward.domain.WardCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWardRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull WardCategory category,
        @Min(0) int capacity,
        @Size(max = 80) String location,
        @Size(max = 500) String description) {
}
