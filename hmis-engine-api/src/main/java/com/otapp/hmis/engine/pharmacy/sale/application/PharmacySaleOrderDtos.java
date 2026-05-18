package com.otapp.hmis.engine.pharmacy.sale.application;

import com.otapp.hmis.engine.patient.domain.PaymentType;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleLineStatus;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PharmacySaleOrderDtos {

    private PharmacySaleOrderDtos() {}

    public record PharmacySaleOrderLineDto(
            String uid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            String medicineStrength,
            int quantity,
            String dose,
            String frequency,
            Integer durationDays,
            String instructions,
            BigDecimal unitPrice,
            BigDecimal lineAmount,
            PharmacySaleLineStatus status,
            Instant acceptedAt,
            Instant heldAt,
            Instant verifiedAt,
            Instant approvedAt,
            Instant soldAt,
            Instant rejectedAt,
            String rejectReason,
            String cancelReason,
            Instant createdAt) {}

    public record PharmacySaleOrderDto(
            String uid,
            String saleNo,
            String pharmacyUid,
            String pharmacyName,
            String patientUid,
            String patientNo,
            String customerName,
            String customerPhone,
            PharmacySaleOrderStatus status,
            PaymentType paymentType,
            String insurancePlanUid,
            String insurancePlanName,
            String currency,
            BigDecimal subtotal,
            BigDecimal totalPaid,
            BigDecimal balance,
            Instant openedAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,
            Instant createdAt,
            Instant updatedAt,
            List<PharmacySaleOrderLineDto> lines) {}

    public record PharmacySaleOrderSummary(
            String uid,
            String saleNo,
            String pharmacyName,
            String customerName,
            String patientUid,
            PharmacySaleOrderStatus status,
            PaymentType paymentType,
            BigDecimal subtotal,
            BigDecimal totalPaid,
            BigDecimal balance,
            String currency,
            Instant openedAt,
            Instant completedAt) {}

    public record AddLineRequest(
            @NotBlank @Size(min = 26, max = 26) String medicineUid,
            @Min(1) int quantity,
            @Size(max = 80) String dose,
            @Size(max = 80) String frequency,
            @Min(0) Integer durationDays,
            @Size(max = 500) String instructions,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice) {}

    public record CreatePharmacySaleOrderRequest(
            @NotBlank @Size(min = 26, max = 26) String pharmacyUid,
            @NotBlank @Size(max = 160) String customerName,
            @Size(max = 40) String customerPhone,
            @Size(min = 26, max = 26) String patientUid,
            @NotNull PaymentType paymentType,
            @Size(min = 26, max = 26) String insurancePlanUid,
            @NotEmpty @Valid List<AddLineRequest> lines) {}

    public record CancelSaleRequest(@Size(max = 255) String reason) {}

    public record RejectLineRequest(@Size(max = 255) String reason) {}
}
