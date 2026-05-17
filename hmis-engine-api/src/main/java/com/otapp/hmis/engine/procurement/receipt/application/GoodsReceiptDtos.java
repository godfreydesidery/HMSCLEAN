package com.otapp.hmis.engine.procurement.receipt.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class GoodsReceiptDtos {

    private GoodsReceiptDtos() {}

    public record GoodsReceiptLineDto(
            String uid,
            String poLineUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            int quantity) {}

    public record GoodsReceiptDto(
            String uid,
            String receiptNo,
            String orderUid,
            String orderNo,
            String pharmacyUid,
            String pharmacyName,
            String receivedByUsername,
            String deliveryNote,
            String notes,
            Instant receivedAt,
            Instant createdAt,
            List<GoodsReceiptLineDto> lines) {}

    public record ReceiveLineRequest(
            @NotBlank @Size(min = 26, max = 26) String poLineUid,
            @Min(1) int quantity) {}

    public record RecordReceiptRequest(
            @Size(max = 120) String deliveryNote,
            @Size(max = 500) String notes,
            @NotEmpty @Valid List<ReceiveLineRequest> lines) {}
}
