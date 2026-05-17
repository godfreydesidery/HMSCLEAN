package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InvoiceDtos {

    private InvoiceDtos() {}

    public record InvoiceLineDto(
            String uid,
            InvoiceLineKind kind,
            String serviceUid,
            String referenceUid,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount) {}

    public record InvoiceDto(
            String uid,
            String invoiceNo,
            String consultationUid,
            String patientUid,
            String patientName,
            String patientNo,
            PaymentType paymentType,
            String insurancePlanUid,
            String insurancePlanName,
            String currency,
            BigDecimal subtotal,
            BigDecimal totalPaid,
            BigDecimal balance,
            InvoiceStatus status,
            Instant issuedAt,
            Instant paidAt,
            Instant cancelledAt,
            String cancelReason,
            Instant createdAt,
            Instant updatedAt,
            List<InvoiceLineDto> lines,
            List<PaymentDto> payments) {}

    public record InvoiceSummary(
            String uid,
            String invoiceNo,
            String consultationUid,
            String patientUid,
            String patientName,
            String patientNo,
            InvoiceStatus status,
            PaymentType paymentType,
            BigDecimal subtotal,
            BigDecimal totalPaid,
            BigDecimal balance,
            String currency,
            Instant issuedAt,
            Instant createdAt) {}

    public record RecordPaymentRequest(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 80) String reference,
            @Size(max = 255) String note) {}

    public record PaymentDto(
            String uid,
            String paymentNo,
            PaymentMethod method,
            BigDecimal amount,
            String currency,
            String reference,
            String note,
            Instant receivedAt,
            Instant createdAt) {}

    public record CancelInvoiceRequest(@Size(max = 255) String reason) {}
}
