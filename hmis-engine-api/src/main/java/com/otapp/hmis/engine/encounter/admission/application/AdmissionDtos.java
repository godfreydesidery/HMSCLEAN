package com.otapp.hmis.engine.encounter.admission.application;

import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AdmissionDtos {

    private AdmissionDtos() {}

    public record AdmissionDto(
            String uid,
            String admissionNo,

            String patientUid,
            String patientNo,
            String patientName,

            String wardUid,
            String wardName,
            String bedUid,
            String bedLabel,

            String admittingClinicianUsername,
            String admittingClinicianName,

            AdmissionStatus status,
            PaymentType paymentType,
            String insurancePlanUid,
            String insurancePlanName,

            String consultationUid,
            String consultationNo,

            String admissionReason,
            Instant admittedAt,
            Instant dischargedAt,
            String dischargeSummary,
            Instant cancelledAt,
            String cancelReason,

            Instant createdAt,
            Instant updatedAt) {}

    public record AdmissionSummary(
            String uid,
            String admissionNo,
            String patientUid,
            String patientNo,
            String patientName,
            String wardName,
            String bedLabel,
            String admittingClinicianName,
            AdmissionStatus status,
            Instant admittedAt,
            Instant dischargedAt) {}

    public record AdmitPatientRequest(
            @NotBlank @Size(min = 26, max = 26) String patientUid,
            @NotBlank @Size(min = 26, max = 26) String wardUid,
            /** Optional — when set, claims the bed and overrides bedLabel from {@code Bed.label}. */
            @Size(min = 26, max = 26) String bedUid,
            /** Free-text bed label used only when {@code bedUid} is null. */
            @Size(max = 32) String bedLabel,
            @NotBlank @Size(max = 64) String admittingClinicianUsername,
            @NotNull PaymentType paymentType,
            @Size(min = 26, max = 26) String insurancePlanUid,
            @Size(min = 26, max = 26) String consultationUid,
            @Size(max = 500) String admissionReason) {}

    public record TransferWardRequest(
            @NotBlank @Size(min = 26, max = 26) String wardUid,
            /** Optional — when set, claims the bed in the target ward and overrides bedLabel. */
            @Size(min = 26, max = 26) String bedUid,
            @Size(max = 32) String bedLabel) {}

    public record DischargeRequest(@Size(max = 1000) String summary) {}

    public record CancelAdmissionRequest(@Size(max = 255) String reason) {}
}
