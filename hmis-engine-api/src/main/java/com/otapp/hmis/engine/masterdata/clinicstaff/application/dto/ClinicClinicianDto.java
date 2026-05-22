package com.otapp.hmis.engine.masterdata.clinicstaff.application.dto;

import java.time.Instant;

/**
 * A clinician affiliated with a clinic, enriched with the clinician's display
 * name and (when present) clinical specialty so the UI can render
 * "Dr. X — Cardiology".
 */
public record ClinicClinicianDto(
        String uid,
        String clinicUid,
        String userUid,
        String username,
        String fullName,
        String specialty,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
