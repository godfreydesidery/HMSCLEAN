package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceScope;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.billing.invoice.domain.LineCoverageStatus;
import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
            BigDecimal amount,
            /** Cash applied to this line; {@code paidAmount == amount} means fully paid. */
            BigDecimal paidAmount,
            /** Cash still owed on this line ({@code amount - paidAmount}); zero for COVERED. */
            BigDecimal outstanding,
            /** Negotiable floor for this line's unit price (null = no lower bound). */
            BigDecimal minUnitPrice,
            /** Negotiable ceiling for this line's unit price (null = no upper bound). */
            BigDecimal maxUnitPrice,
            /** Whether the unit price may still be renegotiated (no payment taken yet). */
            boolean priceOverridable,
            /** Payer routing: COVERED (insurer pays), VERIFIED (insured-but-uncovered inpatient, owed), UNPAID (cash). */
            LineCoverageStatus coverageStatus,
            /** Membership number stamped on a COVERED line; null otherwise. */
            String membershipNo,
            /** The plan that covered this line; null for self-pay / cash. */
            String payerPlanUid,
            /** Set on a supplementary top-up line, pointing at its COVERED principal; null otherwise. */
            String principalLineUid) {}

    public record InvoiceDto(
            String uid,
            String invoiceNo,
            InvoiceScope scope,
            String consultationUid,
            String admissionUid,
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
            InvoiceScope scope,
            String consultationUid,
            String admissionUid,
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

    /**
     * The admission billing picture surfaced to the cashier / ward-admin closure UI:
     * the admission invoice's money math plus the {@code cleared} flag the discharge
     * gate enforces. {@code cleared} is true when the invoice is fully settled
     * (balance == 0); a positive balance blocks discharge / referral / deceased closure.
     */
    public record AdmissionBillingSummaryDto(
            Long id,
            String invoiceUid,
            String invoiceNo,
            InvoiceStatus status,
            BigDecimal subtotal,
            BigDecimal totalPaid,
            BigDecimal totalCredited,
            BigDecimal balance,
            boolean cleared) {}

    /** Lightweight gate read for the cashier UI: does this admission still owe money? */
    public record AdmissionOutstandingDto(boolean hasOutstanding, BigDecimal balance) {}

    /** Negotiate a line's unit price within the service's [min, max] band. */
    public record OverrideLinePriceRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal unitPrice) {}

    /**
     * One cash-payable line in a patient's cashier queue (legacy UNPAID
     * {@code PatientBill}), flattened across the patient's open invoices with its
     * parent invoice's identity. The cashier ticks these and collects
     * {@code outstanding} per ticked line.
     */
    public record PayableLineDto(
            String invoiceUid,
            String invoiceNo,
            InvoiceScope scope,
            String lineUid,
            InvoiceLineKind kind,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            BigDecimal paidAmount,
            BigDecimal outstanding,
            LineCoverageStatus coverageStatus,
            String currency) {}

    /**
     * Collect cash for a selected set of a patient's payable lines (legacy
     * {@code confirm_bills_payment} over the checked bills). Each selected line is
     * settled to its full outstanding — legacy pays a bill in full or not at all.
     * Lines may span several invoices; the service groups them and writes one
     * payment per invoice.
     */
    public record PayLinesRequest(
            @NotNull PaymentMethod method,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 80) String reference,
            @Size(max = 255) String note,
            @NotEmpty List<@NotBlank String> lineUids) {}

    /** Outcome of a cashier line-level collection: what was taken, over which invoices. */
    public record PayLinesResult(
            BigDecimal totalCollected,
            String currency,
            int lineCount,
            List<InvoiceDto> invoices) {}
}
