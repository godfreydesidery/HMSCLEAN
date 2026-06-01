package com.otapp.hmis.engine.reporting.application;

import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReportingDtos {

    private ReportingDtos() {}

    // ----- Revenue ---------------------------------------------------------

    public record RevenueByKindEntry(InvoiceLineKind kind, BigDecimal amount) {}

    public record RevenueSummaryDto(
            LocalDate from,
            LocalDate to,
            BigDecimal totalBilled,
            BigDecimal totalCollected,
            BigDecimal totalCredited,
            BigDecimal totalRefunded,
            BigDecimal netRevenue,
            List<RevenueByKindEntry> breakdownByKind) {}

    // ----- IPD register ---------------------------------------------------

    /** One row on the IPD register — lighter than AdmissionDto, tuned for reporting columns. */
    public record IpdRegisterEntry(
            String admissionUid,
            String admissionNo,
            String patientUid,
            String patientName,
            String wardUid,
            String wardName,
            String bedLabel,
            String admittingClinicianUsername,
            String status,
            Instant admittedAt,
            Instant dischargedAt) {}

    // ----- Bed occupancy --------------------------------------------------

    public record BedOccupancyEntry(
            String wardUid,
            String wardName,
            int capacity,
            long beds,
            long occupied,
            long free,
            long outOfService) {}

    // ----- Stock reports --------------------------------------------------

    public record StockOutEntry(
            String locationUid,    // pharmacy or store uid
            String locationName,
            String locationKind,   // "PHARMACY" or "STORE"
            String medicineUid,
            String medicineCode,
            String medicineName) {}

    public record ExpiringBatchEntry(
            String batchUid,
            String locationUid,
            String locationName,
            String locationKind,   // "PHARMACY" or "STORE"
            String medicineUid,
            String medicineCode,
            String medicineName,
            String batchNo,
            LocalDate expiresAt,
            int quantity) {}

    // ----- Collections / cash-up (BILL-2) ---------------------------------

    /** A method's slice of a cashier's (or the day's) takings. */
    public record MethodAmountEntry(PaymentMethod method, BigDecimal amount, long count) {}

    /** One cashier's takings over the range, with a per-method breakdown. */
    public record CashierCollectionEntry(
            String cashierUsername,
            String cashierName,
            long paymentCount,
            BigDecimal totalCollected,
            BigDecimal cashCollected,
            List<MethodAmountEntry> byMethod) {}

    public record CollectionsReportDto(
            LocalDate from,
            LocalDate to,
            BigDecimal totalCollected,
            BigDecimal totalCash,
            long paymentCount,
            List<CashierCollectionEntry> cashiers) {}

    // ----- Revenue by payment mode (BILL-5) -------------------------------

    public record RevenueByModeDto(
            LocalDate from,
            LocalDate to,
            BigDecimal totalCollected,
            List<MethodAmountEntry> byMethod) {}

    // ----- Pharmacy sales (BILL-5) ----------------------------------------

    public record PharmacySalesEntry(
            String medicineUid,
            String medicineCode,
            String medicineName,
            BigDecimal quantity,
            BigDecimal amount,
            long lineCount) {}

    public record PharmacySalesDto(
            LocalDate from,
            LocalDate to,
            BigDecimal totalQuantity,
            BigDecimal totalAmount,
            List<PharmacySalesEntry> items) {}
}
