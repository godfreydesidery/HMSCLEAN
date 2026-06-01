package com.otapp.hmis.engine.encounter.consultation.application;

import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationTransferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ConsultationTransferDtos {

    private ConsultationTransferDtos() {}

    /**
     * A consultation-transfer row, enriched with denormalised display fields for
     * the receiving queue (patientNo / patientName, targetClinicName,
     * sourceConsultationNo). Carries both numeric {@code id} and {@code uid}.
     */
    public record ConsultationTransferDto(
            Long id,
            String uid,

            String sourceConsultationUid,
            String sourceConsultationNo,

            String patientUid,
            String patientNo,
            String patientName,

            String targetClinicUid,
            String targetClinicName,

            ConsultationTransferStatus status,
            String reason,

            String createdConsultationUid,
            String cancelReason,

            Instant completedAt,
            Instant cancelledAt,

            Instant createdAt,
            Instant updatedAt) {}

    /** Reception pickup — chooses the receiving clinician at the target clinic. */
    public record AcceptTransferRequest(
            @NotBlank @Size(max = 64) String clinicianUsername) {}

    /** Optional revert note recorded by the initiating doctor. */
    public record CancelTransferRequest(@Size(max = 500) String reason) {}
}
