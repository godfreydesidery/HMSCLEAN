package com.otapp.hmis.engine.masterdata.clinic.application;

import com.otapp.hmis.engine.masterdata.clinic.application.dto.ClinicDto;
import com.otapp.hmis.engine.masterdata.clinic.domain.Clinic;

final class ClinicMapper {

    private ClinicMapper() {
    }

    static ClinicDto toDto(Clinic c) {
        return new ClinicDto(
                c.getUid(),
                c.getCode(),
                c.getName(),
                c.getType(),
                c.getDescription(),
                c.getLocation(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
