package com.otapp.hmis.engine.reporting.application;

import com.otapp.hmis.engine.billing.creditnote.domain.CreditNoteRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.payment.domain.PaymentRepository;
import com.otapp.hmis.engine.billing.refund.domain.RefundRepository;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.admission.domain.Admission;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.masterdata.bed.domain.BedRepository;
import com.otapp.hmis.engine.masterdata.bed.domain.BedStatus;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBalance;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBalanceRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBatch;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBatchRepository;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.BedOccupancyEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.ExpiringBatchEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.IpdRegisterEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.RevenueByKindEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.RevenueSummaryDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.StockOutEntry;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBalance;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBalanceRepository;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBatch;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBatchRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only roll-ups for management reports (PROCESS.md §13, §17.12).
 * Everything is computed live — no persisted reporting tables yet. A
 * persisted-daily-summary table can land later if query cost becomes
 * the bottleneck.
 */
@Service
@RequiredArgsConstructor
public class ReportingService {

    private final InvoiceLineRepository invoiceLineRepository;
    private final PaymentRepository paymentRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final RefundRepository refundRepository;

    private final AdmissionRepository admissionRepository;
    private final WardRepository wardRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;

    private final StockBalanceRepository pharmacyStockBalanceRepository;
    private final StoreStockBalanceRepository storeStockBalanceRepository;
    private final StockBatchRepository pharmacyStockBatchRepository;
    private final StoreStockBatchRepository storeStockBatchRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;

    // ==================================================================
    // Revenue
    // ==================================================================

    @Transactional(readOnly = true)
    public RevenueSummaryDto revenueSummary(LocalDate from, LocalDate to) {
        requireRange(from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalBilled    = nz(invoiceLineRepository.sumTotalBilledInIssuedRange(fromInstant, toInstant));
        BigDecimal totalCollected = nz(paymentRepository.sumReceivedInRange(fromInstant, toInstant));
        BigDecimal totalCredited  = nz(creditNoteRepository.sumCreditedInRange(fromInstant, toInstant));
        BigDecimal totalRefunded  = nz(refundRepository.sumRefundedInRange(fromInstant, toInstant));

        // Net revenue = cash in + write-downs (because the write-down represents
        // an authorised "this is what we'll settle for") minus refunds out.
        BigDecimal netRevenue = totalCollected.add(totalCredited).subtract(totalRefunded);

        List<Object[]> rows = invoiceLineRepository.sumAmountByKindInIssuedRange(fromInstant, toInstant);
        List<RevenueByKindEntry> breakdown = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            breakdown.add(new RevenueByKindEntry((InvoiceLineKind) row[0], (BigDecimal) row[1]));
        }

        return new RevenueSummaryDto(from, to,
                totalBilled, totalCollected, totalCredited, totalRefunded, netRevenue,
                breakdown);
    }

    // ==================================================================
    // IPD register
    // ==================================================================

    @Transactional(readOnly = true)
    public List<IpdRegisterEntry> ipdRegister(LocalDate from, LocalDate to,
                                              String wardUid, AdmissionStatus status) {
        requireRange(from, to);
        if (wardUid != null && !wardUid.isBlank()) {
            wardRepository.findByUid(wardUid)
                    .orElseThrow(() -> new NotFoundException("Ward not found: " + wardUid));
        }
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Admission> rows = admissionRepository.ipdRegister(
                fromInstant, toInstant,
                (wardUid == null || wardUid.isBlank()) ? null : wardUid,
                status);

        List<IpdRegisterEntry> out = new ArrayList<>(rows.size());
        for (Admission a : rows) {
            Patient p = patientRepository.findByUid(a.getPatientUid()).orElse(null);
            Ward w    = wardRepository.findByUid(a.getWardUid()).orElse(null);
            out.add(new IpdRegisterEntry(
                    a.getUid(), a.getAdmissionNo(),
                    a.getPatientUid(),
                    p == null ? null : p.fullName(),
                    a.getWardUid(),
                    w == null ? null : w.getName(),
                    a.getBedLabel(),
                    a.getAdmittingClinicianUsername(),
                    a.getStatus().name(),
                    a.getAdmittedAt(),
                    a.getDischargedAt()));
        }
        return out;
    }

    // ==================================================================
    // Bed occupancy
    // ==================================================================

    /**
     * Per-ward bed roll-up. Uses real {@link com.otapp.hmis.engine.masterdata.bed.domain.Bed}
     * status counts; falls back to active-admission counts for wards
     * that don't have beds defined yet (so the report still works
     * during migration).
     */
    @Transactional(readOnly = true)
    public List<BedOccupancyEntry> bedOccupancy() {
        // [wardUid -> [status -> count]] from the bed table.
        Map<String, Map<BedStatus, Long>> bedCountsByWard = new HashMap<>();
        for (Object[] row : bedRepository.countByWardAndStatus()) {
            String wardUid = (String) row[0];
            BedStatus status = (BedStatus) row[1];
            Long count = (Long) row[2];
            bedCountsByWard.computeIfAbsent(wardUid, k -> new EnumMap<>(BedStatus.class)).put(status, count);
        }
        // Fallback for wards with no Bed rows: count ACTIVE (ADMITTED + deposit-pending
        // AWAITING_DEPOSIT) admissions as occupied, consistent with the bed-table branch
        // which counts a RESERVED held bed as occupied.
        Map<String, Long> activeByWard = new HashMap<>();
        for (Object[] row : admissionRepository.countActiveByWard()) {
            activeByWard.put((String) row[0], (Long) row[1]);
        }

        List<Ward> wards = wardRepository.findAll();
        List<BedOccupancyEntry> out = new ArrayList<>(wards.size());
        for (Ward w : wards) {
            Map<BedStatus, Long> beds = bedCountsByWard.getOrDefault(w.getUid(), Map.of());
            long total = beds.values().stream().mapToLong(Long::longValue).sum();
            long occupied;
            long free;
            long outOfService;
            if (total > 0) {
                // A RESERVED bed is held for a deposit-pending (AWAITING_DEPOSIT)
                // admission — it is not available, so it counts as occupied, not free
                // (it would otherwise overstate capacity now that reservations are live).
                occupied     = beds.getOrDefault(BedStatus.OCCUPIED, 0L)
                             + beds.getOrDefault(BedStatus.RESERVED, 0L);
                free         = beds.getOrDefault(BedStatus.FREE, 0L);
                outOfService = beds.getOrDefault(BedStatus.OUT_OF_SERVICE, 0L);
            } else {
                // No Bed rows for this ward yet — use active admissions vs. ward capacity.
                occupied     = activeByWard.getOrDefault(w.getUid(), 0L);
                free         = Math.max(0, w.getCapacity() - occupied);
                outOfService = 0L;
            }
            out.add(new BedOccupancyEntry(
                    w.getUid(), w.getName(), w.getCapacity(),
                    total, occupied, free, outOfService));
        }
        return out;
    }

    // ==================================================================
    // Stock reports
    // ==================================================================

    /**
     * Pharmacy + store balances at or below {@code threshold} (default
     * 0 means "true stock-out"; pass &gt; 0 to surface low-stock items).
     */
    @Transactional(readOnly = true)
    public List<StockOutEntry> stockOut(int threshold) {
        if (threshold < 0) {
            throw new BusinessRuleException("threshold must be >= 0");
        }
        List<StockOutEntry> out = new ArrayList<>();
        for (StockBalance b : pharmacyStockBalanceRepository.findByQuantityLessThanEqual(threshold)) {
            Pharmacy ph = pharmacyRepository.findByUid(b.getPharmacyUid()).orElse(null);
            Medicine m  = medicineRepository.findByUid(b.getMedicineUid()).orElse(null);
            out.add(new StockOutEntry(
                    b.getPharmacyUid(),
                    ph == null ? null : ph.getName(),
                    "PHARMACY",
                    b.getMedicineUid(),
                    m == null ? null : m.getCode(),
                    m == null ? null : m.getName()));
        }
        for (StoreStockBalance b : storeStockBalanceRepository.findByQuantityLessThanEqual(threshold)) {
            Store st   = storeRepository.findByUid(b.getStoreUid()).orElse(null);
            Medicine m = medicineRepository.findByUid(b.getMedicineUid()).orElse(null);
            out.add(new StockOutEntry(
                    b.getStoreUid(),
                    st == null ? null : st.getName(),
                    "STORE",
                    b.getMedicineUid(),
                    m == null ? null : m.getCode(),
                    m == null ? null : m.getName()));
        }
        return out;
    }

    /**
     * Non-empty pharmacy + store batches expiring on or before
     * today + {@code daysAhead}. Pass 0 to see only batches already
     * expired; 90 to plan a quarter ahead.
     */
    @Transactional(readOnly = true)
    public List<ExpiringBatchEntry> expiringBatches(int daysAhead) {
        if (daysAhead < 0) {
            throw new BusinessRuleException("daysAhead must be >= 0");
        }
        LocalDate threshold = LocalDate.now().plusDays(daysAhead);
        List<ExpiringBatchEntry> out = new ArrayList<>();
        for (StockBatch b : pharmacyStockBatchRepository.findExpiringBy(threshold)) {
            Pharmacy ph = pharmacyRepository.findByUid(b.getPharmacyUid()).orElse(null);
            Medicine m  = medicineRepository.findByUid(b.getMedicineUid()).orElse(null);
            out.add(new ExpiringBatchEntry(
                    b.getUid(), b.getPharmacyUid(),
                    ph == null ? null : ph.getName(),
                    "PHARMACY",
                    b.getMedicineUid(),
                    m == null ? null : m.getCode(),
                    m == null ? null : m.getName(),
                    b.getBatchNo(), b.getExpiresAt(), b.getQuantity()));
        }
        for (StoreStockBatch b : storeStockBatchRepository.findExpiringBy(threshold)) {
            Store st   = storeRepository.findByUid(b.getStoreUid()).orElse(null);
            Medicine m = medicineRepository.findByUid(b.getMedicineUid()).orElse(null);
            out.add(new ExpiringBatchEntry(
                    b.getUid(), b.getStoreUid(),
                    st == null ? null : st.getName(),
                    "STORE",
                    b.getMedicineUid(),
                    m == null ? null : m.getCode(),
                    m == null ? null : m.getName(),
                    b.getBatchNo(), b.getExpiresAt(), b.getQuantity()));
        }
        return out;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("from and to are required");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("to cannot be before from");
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
