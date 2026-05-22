package com.otapp.hmis.engine.billing.cashshift.application;

import com.otapp.hmis.engine.billing.cashshift.domain.CashierShiftStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class CashierShiftDtos {

    private CashierShiftDtos() {}

    public record CashierShiftDto(
            String uid,
            String cashierUsername,
            String currency,
            BigDecimal openingFloat,
            Instant openedAt,
            CashierShiftStatus status,
            Instant closedAt,
            BigDecimal closingDeclaredAmount,
            BigDecimal closingExpectedAmount,
            BigDecimal variance,
            String closingNote,
            Instant createdAt,
            Instant updatedAt) {}

    public record OpenShiftRequest(
            @NotNull @DecimalMin(value = "0.00") BigDecimal openingFloat,
            @Size(min = 3, max = 3) String currency) {}

    public record CloseShiftRequest(
            @NotNull @DecimalMin(value = "0.00") BigDecimal closingDeclaredAmount,
            @Size(max = 500) String note) {}
}
