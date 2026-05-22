package com.otapp.hmis.engine.procurement.supplierinvoice.application;

import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoiceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SupplierInvoiceDtos {

    private SupplierInvoiceDtos() {}

    // ----- requests --------------------------------------------------------

    public record CreateSupplierInvoiceRequest(
            @NotBlank @Size(min = 26, max = 26) String orderUid,
            @NotBlank @Size(max = 64) String supplierInvoiceNo,
            @NotNull LocalDate invoiceDate,
            LocalDate dueDate,
            @Size(min = 3, max = 3) String currency,
            @Size(max = 500) String notes,
            @NotEmpty @Valid List<CreateInvoiceLineRequest> lines) {}

    public record CreateInvoiceLineRequest(
            @NotBlank @Size(min = 26, max = 26) String poLineUid,
            @Min(1) int invoicedQuantity,
            @NotNull @DecimalMin(value = "0.00") BigDecimal unitCost) {}

    public record MarkPaidRequest(
            @NotNull PaymentMethod method,
            @Size(max = 120) String reference) {}

    public record ReasonRequest(@Size(max = 255) String reason) {}

    // ----- responses -------------------------------------------------------

    public record InvoiceLineDto(
            String uid,
            String poLineUid,
            String medicineUid,
            String medicineCode,
            String medicineName,
            int poOrderedQuantity,
            int poReceivedQuantity,
            int poInvoicedQuantity,
            int invoicedQuantity,
            BigDecimal unitCost,
            BigDecimal lineAmount) {}

    public record SupplierInvoiceDto(
            String uid,
            String supplierUid,
            String supplierName,
            String orderUid,
            String orderNo,
            String supplierInvoiceNo,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String currency,
            BigDecimal totalAmount,
            SupplierInvoiceStatus status,
            Instant submittedAt, String submittedByUsername,
            Instant approvedAt,  String approvedByUsername,
            Instant paidAt,      String paidByUsername,
            PaymentMethod paymentMethod,
            String paymentReference,
            Instant rejectedAt,  String rejectedByUsername, String rejectReason,
            Instant cancelledAt,
            String notes,
            Instant createdAt, Instant updatedAt,
            List<InvoiceLineDto> lines) {}

    public record SupplierInvoiceSummary(
            String uid,
            String supplierName,
            String orderNo,
            String supplierInvoiceNo,
            LocalDate invoiceDate,
            String currency,
            BigDecimal totalAmount,
            SupplierInvoiceStatus status,
            Instant createdAt) {}
}
