package com.otapp.hmis.engine.encounter.consumable.application;

import com.otapp.hmis.engine.encounter.consumable.domain.ConsumableSourceKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class ConsumableIssueDtos {

    private ConsumableIssueDtos() {}

    public record IssueConsumableRequest(
            @NotBlank @Size(min = 26, max = 26) String consumableUid,
            @NotNull ConsumableSourceKind sourceKind,
            @NotBlank @Size(min = 26, max = 26) String sourceLocationUid,
            @Min(1) int quantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitCost,
            @Size(max = 500) String note) {}

    public record ConsumableIssueDto(
            String uid,
            String admissionUid,
            String consumableUid,
            String consumableCode,
            String consumableName,
            ConsumableSourceKind sourceKind,
            String sourceLocationUid,
            int quantity,
            BigDecimal unitCost,
            BigDecimal lineAmount,
            String issuedByUsername,
            Instant issuedAt,
            String note,
            Instant createdAt) {}
}
