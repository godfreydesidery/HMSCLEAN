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

            String reason,
            Instant bookedAt,
            Instant startedAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,

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
            Instant bookedAt,
            Instant startedAt) {}

    public record StartConsultationRequest(
            @NotBlank @Size(min = 26, max = 26) String patientUid,
            @NotBlank @Size(min = 26, max = 26) String clinicUid,
            @NotBlank @Size(max = 64) String clinicianUsername,
            @NotNull PaymentType paymentType,
            @Size(min = 26, max = 26) String insurancePlanUid,
            @Size(max = 500) String reason) {}

    public record CancelConsultationRequest(@Size(max = 255) String reason) {}
}
