package com.otapp.hmis.engine.masterdata.clinicstaff.application;

import com.otapp.hmis.engine.iam.application.dto.StaffOption;
import com.otapp.hmis.engine.masterdata.clinicstaff.application.dto.ClinicClinicianDto;
import com.otapp.hmis.engine.masterdata.clinicstaff.domain.ClinicClinician;

final class ClinicStaffMapper {

    private ClinicStaffMapper() {
    }

    /**
     * @param staff the clinician's iam projection, or {@code null} if the user
     *              record could not be resolved (the row is still listed, with
     *              the denormalized username as the display fallback).
     */
    static ClinicClinicianDto toDto(ClinicClinician c, StaffOption staff) {
        return new ClinicClinicianDto(
                c.getUid(),
                c.getClinicUid(),
                c.getUserUid(),
                c.getUsername(),
                staff == null ? c.getUsername() : staff.fullName(),
                staff == null ? null : staff.specialty(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
