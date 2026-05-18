package com.otapp.hmis.engine.store.stock.application;

import com.otapp.hmis.engine.store.stock.domain.StoreStockMovementKind;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StoreStockDtos {

    private StoreStockDtos() {}

    /** Per-batch row. The medicine total is the sum across the store's batches. */
    public record StoreStockBatchDto(
            String uid,
            String storeUid,
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
    public record StoreStockBalanceDto(
            String storeUid,
            String storeName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            int totalQuantity,
            int batches,
            LocalDate earliestExpiry,
            List<StoreStockBatchDto> batchDetails) {}

    public record StoreStockMovementDto(
            String uid,
            String storeUid,
            String storeName,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String batchUid,
            String batchNo,
            StoreStockMovementKind kind,
            int quantity,
            int balanceAfter,
            String referenceUid,
            String note,
            String actorUsername,
            Instant occurredAt,
            Instant createdAt) {}

    public record ReceiveStoreStockRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            @NotBlank @Size(max = 64) String batchNo,
            LocalDate expiresAt,
            @Min(1) int quantity,
            @Size(max = 500) String note) {}

    /** Signed adjustment against a specific batch. */
    public record AdjustStoreStockRequest(
            @NotBlank @Size(min = 26, max = 26) String batchUid,
            @NotNull Integer delta,
            @Size(max = 500) String note) {}

    /**
     * One batch consumed during an FEFO issue out of a store. The transfer
     * service uses these to persist per-batch pick rows on the TO line and
     * to propagate batch metadata to the receiving pharmacy.
     */
    public record BatchPickResult(
            String batchUid,
            String batchNo,
            LocalDate expiresAt,
            int quantity,
            String movementUid) {}
}
