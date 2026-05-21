package com.otapp.hmis.engine.iam.application.dto;

/**
 * Lightweight projection of a user used to populate "assign clinician /
 * nurse / pharmacist" pickers in the UI. Username is the stable handle other
 * modules use to reference staff.
 *
 * <p>The clinical attributes ({@code specialty}, {@code registrationNo},
 * {@code licenseNo}) come from the optional {@code ProviderProfile} sidecar and
 * are {@code null} for staff without one — they let pickers render
 * "Dr. X — Cardiology".
 */
public record StaffOption(
        String uid,
        String username,
        String firstName,
        String lastName,
        String fullName,
        String specialty,
        String registrationNo,
        String licenseNo) {
}
