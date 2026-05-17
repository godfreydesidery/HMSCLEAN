package com.otapp.hmis.engine.encounter.result.application;

import com.otapp.hmis.engine.encounter.order.domain.ClinicalOrderKind;
import com.otapp.hmis.engine.encounter.result.domain.OrderResultStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class OrderResultDtos {

    private OrderResultDtos() {}

    public record OrderResultDto(
            String uid,
            String orderUid,
            ClinicalOrderKind orderKind,
            OrderResultStatus status,
            String narrative,
            String impression,
            Instant finalizedAt,
            String finalizedBy,
            Instant amendedAt,
            String amendedBy,
            Instant createdAt,
            Instant updatedAt) {}

    public record SaveResultRequest(
            @Size(max = 8000) String narrative,
            @Size(max = 1000) String impression) {}
}
