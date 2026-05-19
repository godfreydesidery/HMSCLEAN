package com.otapp.hmis.engine.pharmacy.stock.application;

import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementKind;
import com.otapp.hmis.engine.pharmacy.stock.domain.WastageReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StockDtos {

    private StockDtos() {}

    /** Per-batch row. The medicine total is the sum across the pharmacy's batches. */
    public record StockBatchDto(
            String uid,
            String pharmacyUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            String batchNo,
            LocalDate expiresAt,
            boolean expired,
            int quantity,
            Instant receivedAt) {}

    /** Aggregate row used by the list view — sums batches per medicine. */
    public record StockBalanceDto(
            String pharmacyUid,
            String pharmacyName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            int totalQuantity,
            int batches,
            LocalDate earliestExpiry,
            List<StockBatchDto> batchDetails) {}

    public record StockMovementDto(
            String uid,
            String pharmacyUid,
            String pharmacyName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String batchUid,
            String batchNo,
            StockMovementKind kind,
            int quantity,
            int balanceAfter,
            String referenceUid,
            String note,
            String actorUsername,
            Instant occurredAt,
            Instant createdAt) {}

    public record ReceiveStockRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            @NotBlank @Size(max = 64) String batchNo,
            LocalDate expiresAt,
            @Min(1) int quantity,
            /** Optional — when set the quantity is in this unit and is converted to base on save. */
            @Size(min = 26, max = 26) String unitUid,
            @Size(max = 500) String note) {}

    /**
     * Signed adjustment against a specific batch. The batch must already
     * exist (you can't adjust a batch into existence — use receive for that).
     * {@code unitUid} is optional — when set, {@code delta}'s magnitude is
     * scaled by the unit's {@code factorToBase} (sign preserved).
     */
    public record AdjustStockRequest(
            @NotBlank @Size(min = 26, max = 26) String batchUid,
            @NotNull Integer delta,
            @Size(min = 26, max = 26) String unitUid,
            @Size(max = 500) String note) {}

    /**
     * Pharmacist write-off against a specific batch: expired, damaged,
     * recalled, lost, etc. Quantity is positive — the service applies the
     * negative delta. Reason is structured so the shrinkage report can
     * categorise losses. {@code unitUid} is optional and converts to base.
     */
    public record WriteOffStockRequest(
            @NotBlank @Size(min = 26, max = 26) String batchUid,
            @Min(1) int quantity,
            @NotNull WastageReason reason,
            @Size(min = 26, max = 26) String unitUid,
            @Size(max = 500) String note) {}

    /**
     * One batch consumed during an FEFO issue out of a pharmacy. The
     * pharmacy↔pharmacy transfer service uses these to persist per-batch
     * pick rows on the TO line and to propagate batch metadata to the
     * receiving pharmacy.
     */
    public record BatchPickResult(
            String batchUid,
            String batchNo,
            LocalDate expiresAt,
            int quantity,
            String movementUid) {}
}
