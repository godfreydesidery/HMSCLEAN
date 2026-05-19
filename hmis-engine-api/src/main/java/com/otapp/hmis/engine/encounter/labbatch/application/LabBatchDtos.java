package com.otapp.hmis.engine.encounter.labbatch.application;

import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class LabBatchDtos {

    private LabBatchDtos() {}

    public record LabBatchDto(
            String uid,
            String batchNo,
            String labTestTypeUid,
            String labTestCode,
            String labTestName,
            String note,
            LabBatchStatus status,
            String openedByUsername,
            Instant openedAt,
            Instant processedAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,
            int memberCount,
            List<String> memberOrderUids,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateLabBatchRequest(
            @NotBlank @Size(min = 26, max = 26) String labTestTypeUid,
            @Size(max = 500) String note,
            @NotEmpty List<@NotBlank @Size(min = 26, max = 26) String> orderUids) {}

    public record AddOrderRequest(
            @NotBlank @Size(min = 26, max = 26) String orderUid) {}

    public record CancelLabBatchRequest(@Size(max = 255) String reason) {}
}
