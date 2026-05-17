package com.otapp.hmis.engine.iam.application.dto;

/**
 * Lightweight projection of a user used to populate "assign clinician /
 * nurse / pharmacist" pickers in the UI. Username is the stable handle other
 * modules use to reference staff.
 */
public record StaffOption(
        String uid,
        String username,
        String firstName,
        String lastName,
        String fullName) {
}
