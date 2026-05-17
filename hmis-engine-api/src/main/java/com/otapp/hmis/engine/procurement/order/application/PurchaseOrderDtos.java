package com.otapp.hmis.engine.procurement.order.application;

import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseOrderDtos {

    private PurchaseOrderDtos() {}

    public record PurchaseOrderLineDto(
            String uid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            int orderedQuantity,
            int receivedQuantity,
            int outstandingQuantity,
            BigDecimal unitCost,
            String currency,
            BigDecimal lineAmount) {}

    public record PurchaseOrderDto(
            String uid,
            String orderNo,

            String supplierUid,
            String supplierName,

            String storeUid,
            String storeName,

            PurchaseOrderStatus status,
            LocalDate expectedDeliveryDate,
            String notes,

            Instant orderedAt,
            Instant receivedAt,
            Instant cancelledAt,
            String cancelReason,

            String currency,
            BigDecimal subtotal,

            Instant createdAt,
            Instant updatedAt,

            List<PurchaseOrderLineDto> lines) {}

    public record PurchaseOrderSummary(
            String uid,
            String orderNo,
            String supplierName,
            String storeName,
            PurchaseOrderStatus status,
            LocalDate expectedDeliveryDate,
            BigDecimal subtotal,
            String currency,
            Instant createdAt) {}

    public record CreatePurchaseOrderRequest(
            @NotBlank @Size(min = 26, max = 26) String supplierUid,
            @NotBlank @Size(min = 26, max = 26) String storeUid,
            LocalDate expectedDeliveryDate,
            @Size(max = 500) String notes) {}

    public record AddLineRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            @Min(1) int orderedQuantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitCost,
            @Size(max = 3) String currency) {}

    public record UpdateLineRequest(
            @Min(1) int orderedQuantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitCost,
            @Size(max = 3) String currency) {}

    public record CancelPurchaseOrderRequest(@Size(max = 255) String reason) {}
}
