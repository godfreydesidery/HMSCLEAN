package com.otapp.hmis.engine.encounter.prescription.application;

import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineForm;
import com.otapp.hmis.engine.patient.domain.PatientClassScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PrescriptionDtos {

    private PrescriptionDtos() {}

    /** A row in the pharmacy dispensing queue — patient + drug + the action context. */
    public record PrescriptionWorklistRow(
            String uid,
            String prescriptionNo,
            String patientUid,
            String patientNo,
            String patientName,
            PatientClassScope patientClass,
            String consultationUid,
            String medicineName,
            String dose,
            String frequency,
            Integer quantity,
            PrescriptionStatus status,
            boolean settled,
            Instant requestedAt) {}

    public record PrescriptionDto(
            String uid,
            String prescriptionNo,
            String consultationUid,
            String patientUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            MedicineForm medicineForm,
            PrescriptionStatus status,
            String dose,
            String dosageUid,
            String dosageCode,
            String route,
            String routeUid,
            String routeCode,
            String frequency,
            String frequencyUid,
            String frequencyCode,
            Integer frequencyTimesPerDay,
            Integer durationDays,
            Integer quantity,
            String instructions,
            Instant requestedAt,
            Instant acceptedAt,
            Instant heldAt,
            Instant verifiedAt,
            Instant approvedAt,
            Instant dispensedAt,
            Instant rejectedAt,
            String rejectReason,
            String cancelReason,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * Either provide free text ({@code dose}, {@code frequency}, {@code route})
     * or the masterdata picklist uid for each slot. If both are given,
     * the masterdata name wins and the typed string is ignored.
     */
    public record CreatePrescriptionRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            @Size(max = 80) String dose,
            @Size(min = 26, max = 26) String dosageUid,
            @Size(max = 80) String route,
            @Size(min = 26, max = 26) String routeUid,
            @Size(max = 80) String frequency,
            @Size(min = 26, max = 26) String frequencyUid,
            @Min(0) Integer durationDays,
            @Min(0) Integer quantity,
            @Size(max = 500) String instructions) {}

    public record CancelPrescriptionRequest(@Size(max = 255) String reason) {}
}
