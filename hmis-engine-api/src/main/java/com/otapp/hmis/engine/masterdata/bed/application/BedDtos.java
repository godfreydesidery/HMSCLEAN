package com.otapp.hmis.engine.masterdata.bed.application;

import com.otapp.hmis.engine.masterdata.bed.domain.BedStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class BedDtos {

    private BedDtos() {}

    public record BedDto(
            String uid,
            String wardUid,
            String wardName,
            String label,
            String notes,
            BedStatus status,
            String occupiedByAdmissionUid,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateBedRequest(
            @NotBlank @Size(max = 32) String label,
            @Size(max = 500) String notes) {}

    public record UpdateBedRequest(
            @NotBlank @Size(max = 32) String label,
            @Size(max = 500) String notes) {}

    public record OutOfServiceRequest(@Size(max = 500) String reason) {}
}
