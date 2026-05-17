package com.otapp.hmis.engine.masterdata.clinic.application.dto;

import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicType;
import java.time.Instant;

public record ClinicDto(
        String uid,
        String code,
        String name,
        ClinicType type,
        String description,
        String location,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
