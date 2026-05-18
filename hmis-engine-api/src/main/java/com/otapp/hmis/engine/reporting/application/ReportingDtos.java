package com.otapp.hmis.engine.reporting.application;

import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
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
}
