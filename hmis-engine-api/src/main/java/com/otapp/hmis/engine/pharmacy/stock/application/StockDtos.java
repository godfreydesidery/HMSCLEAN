package com.otapp.hmis.engine.pharmacy.stock.application;

import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementKind;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class StockDtos {

    private StockDtos() {}

    public record StockBalanceDto(
            String uid,
            String pharmacyUid,
            String pharmacyName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            int quantity,
            Instant createdAt,
            Instant updatedAt) {}

    public record StockMovementDto(
            String uid,
            String pharmacyUid,
            String pharmacyName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            StockMovementKind kind,
            int quantity,
            int balanceAfter,
            String referenceUid,
            String note,
            String actorUsername,
            Instant occurredAt,
            Instant createdAt) {}

    public record ReceiveStockRequest(
            @Size(min = 26, max = 26) String medicineUid,
            @Min(1) int quantity,
            @Size(max = 500) String note) {}

    public record AdjustStockRequest(
            @Size(min = 26, max = 26) String medicineUid,
            /** Signed delta: positive adds, negative removes. */
            @NotNull Integer delta,
            @Size(max = 500) String note) {}
}
