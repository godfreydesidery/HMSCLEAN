package com.otapp.hmis.engine.reporting.application;

import com.otapp.hmis.engine.billing.creditnote.domain.CreditNoteRepository;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineRepository;
import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
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
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.BedOccupancyEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.CashierCollectionEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.CollectionsReportDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.ExpiringBatchEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.IpdRegisterEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.MethodAmountEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.PharmacySalesDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.PharmacySalesEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.RevenueByKindEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.RevenueByModeDto;
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
import java.util.LinkedHashMap;
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
    private final UserRepository userRepository;

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

    /**
     * Revenue split by payment mode (BILL-5) — how the collected cash came in
     * (CASH / MOBILE_MONEY / CARD / BANK_TRANSFER / INSURANCE_CLAIM / OTHER) over
     * the range. Counts received payments, complementing the by-service-kind
     * breakdown on {@link #revenueSummary}.
     */
    @Transactional(readOnly = true)
    public RevenueByModeDto revenueByMode(LocalDate from, LocalDate to) {
        requireRange(from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<MethodAmountEntry> byMethod = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : paymentRepository.sumByMethodInRange(fromInstant, toInstant)) {
            BigDecimal amount = nz((BigDecimal) row[1]);
            byMethod.add(new MethodAmountEntry((PaymentMethod) row[0], amount, (Long) row[2]));
            total = total.add(amount);
        }
        return new RevenueByModeDto(from, to, total, byMethod);
    }

    // ==================================================================
    // Collections / cash-up (per cashier)
    // ==================================================================

    /**
     * Per-cashier collections / cash-up (BILL-2): every cashier's takings over
     * the range, with a per-method breakdown and the cash subtotal (the figure
     * a till should reconcile to). Driven by {@code Payment.createdBy}.
     */
    @Transactional(readOnly = true)
    public CollectionsReportDto collections(LocalDate from, LocalDate to) {
        requireRange(from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Accumulate per cashier, preserving the query's createdBy ordering.
        Map<String, CashierAccumulator> byCashier = new LinkedHashMap<>();
        for (Object[] row : paymentRepository.collectionsByUserAndMethodInRange(fromInstant, toInstant)) {
            String username = (String) row[0];
            PaymentMethod method = (PaymentMethod) row[1];
            BigDecimal amount = nz((BigDecimal) row[2]);
            long count = (Long) row[3];
            byCashier.computeIfAbsent(username, CashierAccumulator::new).add(method, amount, count);
        }

        List<CashierCollectionEntry> cashiers = new ArrayList<>(byCashier.size());
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandCash = BigDecimal.ZERO;
        long grandCount = 0;
        for (CashierAccumulator acc : byCashier.values()) {
            String name = acc.username == null ? null
                    : userRepository.findByUsername(acc.username).map(User::fullName).orElse(null);
            cashiers.add(new CashierCollectionEntry(
                    acc.username, name, acc.count, acc.total, acc.cash, acc.methods()));
            grandTotal = grandTotal.add(acc.total);
            grandCash = grandCash.add(acc.cash);
            grandCount += acc.count;
        }
        return new CollectionsReportDto(from, to, grandTotal, grandCash, grandCount, cashiers);
    }

    /** Mutable per-cashier tally used while folding the grouped query rows. */
    private static final class CashierAccumulator {
        private final String username;
        private final List<MethodAmountEntry> byMethod = new ArrayList<>();
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal cash = BigDecimal.ZERO;
        private long count = 0;

        private CashierAccumulator(String username) { this.username = username; }

        private void add(PaymentMethod method, BigDecimal amount, long c) {
            byMethod.add(new MethodAmountEntry(method, amount, c));
            total = total.add(amount);
            if (method == PaymentMethod.CASH) { cash = cash.add(amount); }
            count += c;
        }

        private List<MethodAmountEntry> methods() { return byMethod; }
    }

    // ==================================================================
    // Pharmacy sales
    // ==================================================================

    /**
     * Pharmacy sales (BILL-5): medicines sold (MEDICINE invoice lines on issued
     * invoices) over the range, grouped by medicine with quantity + revenue,
     * highest-revenue first.
     */
    @Transactional(readOnly = true)
    public PharmacySalesDto pharmacySales(LocalDate from, LocalDate to) {
        requireRange(from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<PharmacySalesEntry> items = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Object[] row : invoiceLineRepository.pharmacySalesInRange(fromInstant, toInstant)) {
            String medicineUid = (String) row[0];
            BigDecimal qty = nz((BigDecimal) row[1]);
            BigDecimal amount = nz((BigDecimal) row[2]);
            Medicine m = medicineUid == null ? null
                    : medicineRepository.findByUid(medicineUid).orElse(null);
            items.add(new PharmacySalesEntry(
                    medicineUid,
                    m == null ? null : m.getCode(),
                    m == null ? null : m.getName(),
                    qty, amount, (Long) row[3]));
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(amount);
        }
        items.sort((a, b) -> b.amount().compareTo(a.amount()));
        return new PharmacySalesDto(from, to, totalQty, totalAmount, items);
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
