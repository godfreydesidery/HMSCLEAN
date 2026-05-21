package com.otapp.hmis.engine.encounter.medadmin.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class MedicationAdministrationDtos {

    private MedicationAdministrationDtos() {}

    public record MedicationAdministrationDto(
            String uid,
            String admissionUid,
            String prescriptionUid,
            String prescriptionNo,
            String patientUid,
            String medicineUid,
            String medicineName,
            String doseGiven,
            String route,
            String patientResponse,
            String notes,
            Instant administeredAt,
            String administeredByUsername,
            Instant createdAt) {}

    /**
     * Record one administered dose. {@code administeredAt} is optional — defaults
     * to now. The administering nurse is taken from the authenticated user.
     */
    public record RecordAdministrationRequest(
            @NotBlank @Size(min = 26, max = 26) String prescriptionUid,
            @NotBlank @Size(max = 120) String doseGiven,
            @Size(max = 80)  String route,
            @Size(max = 500) String patientResponse,
            @Size(max = 1000) String notes,
            Instant administeredAt) {}
}
