package com.otapp.hmis.engine.encounter.consultation.application;

import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ConsultationDtos {

    private ConsultationDtos() {}

    public record ConsultationDto(
            String uid,
            String consultationNo,

            String patientUid,
            String patientNo,
            String patientName,

            String clinicUid,
            String clinicName,

            String clinicianUsername,
            String clinicianName,

            ConsultationStatus status,
            PaymentType paymentType,
            String insurancePlanUid,
            String insurancePlanName,

            /** TRUE once the consultation fee is settled (denormalised gate). */
            boolean feeSettled,
            /** TRUE while clinical entries are permitted (status == IN_PROGRESS). */
            boolean authorable,

            String reason,
            Instant bookedAt,
            Instant startedAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,

            String followUpOfConsultationUid,
            String transferredToConsultationUid,
            String transferredFromConsultationUid,
            String transferReason,
            Instant transferredAt,

            Instant createdAt,
            Instant updatedAt) {}

    public record ConsultationSummary(
            String uid,
            String consultationNo,
            String patientUid,
            String patientNo,
            String patientName,
            String clinicName,
            String clinicianName,
            ConsultationStatus status,
            PaymentType paymentType,
            boolean feeSettled,
            Instant bookedAt,
            Instant startedAt) {}

    public record StartConsultationRequest(
            @NotBlank @Size(min = 26, max = 26) String patientUid,
            @NotBlank @Size(min = 26, max = 26) String clinicUid,
            @NotBlank @Size(max = 64) String clinicianUsername,
            @NotNull PaymentType paymentType,
            @Size(min = 26, max = 26) String insurancePlanUid,
            @Size(max = 500) String reason,
            /** Optional — when set, marks this visit as a follow-up to the referenced consultation. */
            @Size(min = 26, max = 26) String followUpOfConsultationUid) {}

    public record CancelConsultationRequest(@Size(max = 255) String reason) {}

    /**
     * Raise a pending transfer to another <em>clinic</em> (OPC-1). The receiving
     * clinician is chosen later at pickup — legacy-faithful — so this carries the
     * target clinic only.
     */
    public record TransferConsultationRequest(
            @NotBlank @Size(min = 26, max = 26) String targetClinicUid,
            @Size(max = 500) String reason) {}
}
