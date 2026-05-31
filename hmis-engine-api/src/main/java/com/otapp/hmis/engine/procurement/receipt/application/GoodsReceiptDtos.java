package com.otapp.hmis.engine.procurement.receipt.application;

import com.otapp.hmis.engine.procurement.receipt.domain.GoodsReceiptStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class GoodsReceiptDtos {

    private GoodsReceiptDtos() {}

    public record GoodsReceiptLineDto(
            String uid,
            String poLineUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            int quantity,
            String batchNo,
            LocalDate manufacturedDate,
            LocalDate expiresAt) {}

    public record GoodsReceiptDto(
            String uid,
            String receiptNo,
            String orderUid,
            String orderNo,
            String storeUid,
            String storeName,
            String receivedByUsername,
            String deliveryNote,
            String notes,
            GoodsReceiptStatus status,
            Instant receivedAt,
            Instant verifiedAt,
            String verifiedByUsername,
            Instant approvedAt,
            String approvedByUsername,
            Instant rejectedAt,
            String rejectedByUsername,
            String rejectReason,
            Instant createdAt,
            List<GoodsReceiptLineDto> lines) {}

    public record ReceiveLineRequest(
            @NotBlank @Size(min = 26, max = 26) String poLineUid,
            @Min(1) int quantity,
            /** Optional — when set the quantity is in this unit and is converted to base before credit. */
            @Size(min = 26, max = 26) String unitUid,
            @NotBlank @Size(max = 64) String batchNo,
            @PastOrPresent LocalDate manufacturedDate,
            LocalDate expiresAt) {}

    public record RecordReceiptRequest(
            @Size(max = 120) String deliveryNote,
            @Size(max = 500) String notes,
            @NotEmpty @Valid List<ReceiveLineRequest> lines) {}

    public record RejectReceiptRequest(@Size(max = 255) String reason) {}
}
