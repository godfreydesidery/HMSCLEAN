package com.otapp.hmis.engine.masterdata.clinicstaff.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignClinicianRequest(
        @NotBlank
        @Size(max = 26)
        String userUid) {
}
