package com.otapp.hmis.engine.pharmacy.stock.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.masterdata.medicine.application.UnitConversionService;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnit;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleLineStatus;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrder;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderLine;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderLineRepository;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderRepository;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.AdjustStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.BatchPickResult;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.ReceiveStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockBalanceDto;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockBatchDto;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockMovementDto;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.WriteOffStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBalance;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBalanceRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBatch;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBatchRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovement;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementKind;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.WastageReason;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final EnumSet<PharmacySaleLineStatus> TERMINAL_SALE_LINE_STATUSES = EnumSet.of(
            PharmacySaleLineStatus.SOLD,
            PharmacySaleLineStatus.REJECTED,
            PharmacySaleLineStatus.CANCELLED);

    private final StockBalanceRepository balanceRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PharmacySaleOrderRepository saleRepository;
    private final PharmacySaleOrderLineRepository saleLineRepository;
    private final UnitConversionService unitConversion;

    // ----- receive ----------------------------------------------------------

    @Transactional
    public StockBatchDto receive(String pharmacyUid, ReceiveStockRequest request) {
        if (request.quantity() <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        MedicineUnit unit = unitConversion.resolveUnit(request.medicineUid(), request.unitUid());
        int baseQty = unitConversion.toBaseQuantity(unit, request.quantity());
        return doReceive(pharmacyUid, request.medicineUid(), request.batchNo(),
                null, request.expiresAt(), baseQty,
                StockMovementKind.RECEIPT, null, emptyToNull(request.note()));
    }

    /**
     * Cross-module entry point used by the pharmacy↔store transfer service
     * when an RN is completed. Records a {@code TRANSFER_IN} movement
     * referencing the source TO so the stock card explains the receipt.
     * Back-compatible overload that drops the source batch's manufactured
     * date — prefer the 8-arg form so traceability survives the hop.
     */
    @Transactional
    public StockBatchDto receiveFromStore(String pharmacyUid, String medicineUid,
                                          String batchNo, LocalDate expiresAt,
                                          int quantity, String referenceUid, String note) {
        return receiveFromStore(pharmacyUid, medicineUid, batchNo, null, expiresAt,
                quantity, referenceUid, note);
    }

    /**
     * Cross-module entry point used by the pharmacy↔store transfer service,
     * carrying the source batch's manufactured date so it survives the hop
     * onto the destination pharmacy batch.
     */
    @Transactional
    public StockBatchDto receiveFromStore(String pharmacyUid, String medicineUid,
                                          String batchNo, LocalDate manufacturedDate,
                                          LocalDate expiresAt, int quantity,
                                          String referenceUid, String note) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        return doReceive(pharmacyUid, medicineUid, batchNo, manufacturedDate, expiresAt, quantity,
                StockMovementKind.TRANSFER_IN, emptyToNull(referenceUid), emptyToNull(note));
    }

    private StockBatchDto doReceive(String pharmacyUid, String medicineUid, String batchNo,
                                    LocalDate manufacturedDate, LocalDate expiresAt, int quantity,
                                    StockMovementKind kind, String referenceUid, String note) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        Medicine medicine = activeMedicine(medicineUid);

        StockBatch batch = batchRepository
                .findByPharmacyUidAndMedicineUidAndBatchNo(pharmacy.getUid(), medicine.getUid(), batchNo)
                .orElseGet(() -> batchRepository.save(
                        new StockBatch(pharmacy.getUid(), medicine.getUid(), batchNo,
                                manufacturedDate, expiresAt)));
        // If the caller supplied an expiry for an existing batch and it differs,
        // prefer the supplied one (typo correction). Otherwise keep current.
        if (expiresAt != null && !expiresAt.equals(batch.getExpiresAt())) {
            batch.setExpiresAt(expiresAt);
        }
        if (manufacturedDate != null && !manufacturedDate.equals(batch.getManufacturedDate())) {
            batch.setManufacturedDate(manufacturedDate);
        }
        batch.applyDelta(quantity);

        StockBalance balance = lockOrCreateBalance(pharmacy.getUid(), medicine.getUid());
        balance.applyDelta(quantity);

        recordMovement(balance, batch, kind, quantity, referenceUid, note);
        return toBatchDto(batch, medicine);
    }

    // ----- issue to another pharmacy (cross-module entry) ------------------

    /**
     * FEFO-walk a source pharmacy's batches for {@code medicineUid},
     * decrementing each in turn until {@code requested} units have been
     * pulled. Returns one {@link BatchPickResult} per batch consumed so
     * the caller (the pharmacy↔pharmacy transfer service) can persist
     * matching pick rows and propagate batch metadata to the receiving
     * pharmacy.
     *
     * <p>Records {@code TRANSFER_OUT} movements on the source pharmacy.
     * The destination pharmacy's stock is NOT touched here — that happens
     * later when the receiver signs the RN via
     * {@link #receiveFromPharmacy}.
     */
    @Transactional
    public List<BatchPickResult> issueToPharmacy(String sourcePharmacyUid, String medicineUid,
                                                 int requested, String referenceUid, String note) {
        if (requested <= 0) {
            throw new BusinessRuleException("Issue quantity must be positive");
        }
        Pharmacy pharmacy = activePharmacy(sourcePharmacyUid);
        Medicine medicine = activeMedicine(medicineUid);

        List<StockBatch> batches = batchRepository.lockFefoForDispense(pharmacy.getUid(), medicine.getUid());
        int onHand = batches.stream().mapToInt(StockBatch::getQuantity).sum();
        if (onHand < requested) {
            throw new BusinessRuleException(
                    "Insufficient stock at source pharmacy: on hand " + onHand
                            + ", requested " + requested);
        }

        StockBalance balance = balanceRepository
                .lockByPharmacyUidAndMedicineUid(pharmacy.getUid(), medicine.getUid())
                .orElseThrow(() -> new BusinessRuleException("No balance row for medicine"));

        List<BatchPickResult> picks = new ArrayList<>();
        int remaining = requested;
        for (StockBatch batch : batches) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getQuantity());
            if (take > 0) {
                batch.applyDelta(-take);
                balance.applyDelta(-take);
                StockMovement movement = recordMovement(balance, batch,
                        StockMovementKind.TRANSFER_OUT, -take,
                        emptyToNull(referenceUid), emptyToNull(note));
                picks.add(new BatchPickResult(
                        batch.getUid(), batch.getBatchNo(), batch.getManufacturedDate(),
                        batch.getExpiresAt(), take, movement.getUid()));
                remaining -= take;
            }
        }
        if (remaining > 0) {
            throw new BusinessRuleException("Could not fulfil " + requested + ": " + remaining + " short");
        }
        return picks;
    }

    /**
     * Cross-module entry point used by the pharmacy↔pharmacy transfer
     * service when an RN is completed. Records a {@code TRANSFER_IN}
     * movement referencing the source TO so the stock card explains the
     * receipt — same machinery as {@link #receiveFromStore} but used by
     * the other transfer chain for clearer movement provenance.
     */
    @Transactional
    public StockBatchDto receiveFromPharmacy(String pharmacyUid, String medicineUid,
                                             String batchNo, LocalDate expiresAt,
                                             int quantity, String referenceUid, String note) {
        return receiveFromPharmacy(pharmacyUid, medicineUid, batchNo, null, expiresAt,
                quantity, referenceUid, note);
    }

    /**
     * Cross-module entry point used by the pharmacy↔pharmacy transfer
     * service, carrying the source batch's manufactured date so it survives
     * the hop onto the destination pharmacy batch.
     */
    @Transactional
    public StockBatchDto receiveFromPharmacy(String pharmacyUid, String medicineUid,
                                             String batchNo, LocalDate manufacturedDate,
                                             LocalDate expiresAt, int quantity,
                                             String referenceUid, String note) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        return doReceive(pharmacyUid, medicineUid, batchNo, manufacturedDate, expiresAt, quantity,
                StockMovementKind.TRANSFER_IN, emptyToNull(referenceUid), emptyToNull(note));
    }

    // ----- adjust -----------------------------------------------------------

    @Transactional
    public StockBatchDto adjust(String pharmacyUid, AdjustStockRequest request) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        if (request.delta() == 0) {
            throw new BusinessRuleException("Adjustment delta must be non-zero");
        }
        StockBatch batch = batchRepository.findByUid(request.batchUid())
                .orElseThrow(() -> new NotFoundException("Batch not found: " + request.batchUid()));
        if (!batch.getPharmacyUid().equals(pharmacy.getUid())) {
            throw new BusinessRuleException("Batch belongs to a different pharmacy");
        }
        Medicine medicine = activeMedicine(batch.getMedicineUid());

        // Scale magnitude by unit factor, preserve sign. Null unitUid keeps the
        // legacy "delta is in base units" behaviour for callers that haven't migrated.
        int baseDelta = request.delta();
        if (request.unitUid() != null && !request.unitUid().isBlank()) {
            MedicineUnit unit = unitConversion.resolveUnit(medicine.getUid(), request.unitUid());
            int magnitude = unitConversion.toBaseQuantity(unit, Math.abs(request.delta()));
            baseDelta = request.delta() < 0 ? -magnitude : magnitude;
        }

        batch.applyDelta(baseDelta);
        StockBalance balance = lockOrCreateBalance(pharmacy.getUid(), medicine.getUid());
        balance.applyDelta(baseDelta);

        recordMovement(balance, batch, StockMovementKind.ADJUSTMENT, baseDelta,
                null, emptyToNull(request.note()));
        return toBatchDto(batch, medicine);
    }

    // ----- write-off (WASTAGE) ---------------------------------------------

    /**
     * Pharmacist write-off against a specific batch — expired, damaged,
     * recalled, lost, etc. Decrements the batch + balance and records a
     * WASTAGE movement carrying the structured {@link WastageReason} so the
     * shrinkage report can categorise losses.
     */
    @Transactional
    public StockBatchDto writeOff(String pharmacyUid, WriteOffStockRequest request) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        StockBatch batch = batchRepository.findByUid(request.batchUid())
                .orElseThrow(() -> new NotFoundException("Batch not found: " + request.batchUid()));
        if (!batch.getPharmacyUid().equals(pharmacy.getUid())) {
            throw new BusinessRuleException("Batch belongs to a different pharmacy");
        }
        Medicine medicine = activeMedicine(batch.getMedicineUid());

        MedicineUnit unit = unitConversion.resolveUnit(medicine.getUid(), request.unitUid());
        int baseQty = unitConversion.toBaseQuantity(unit, request.quantity());

        batch.applyDelta(-baseQty);
        StockBalance balance = lockOrCreateBalance(pharmacy.getUid(), medicine.getUid());
        balance.applyDelta(-baseQty);

        StockMovement movement = recordMovement(balance, batch, StockMovementKind.WASTAGE,
                -baseQty, null, emptyToNull(request.note()));
        movement.setWastageReason(request.reason());
        return toBatchDto(batch, medicine);
    }

    /**
     * Writes off short-received units at the RECEIVING pharmacy as a transit
     * loss. Used by the transfer chains when an RN records fewer units than
     * the TO issued: the full issued quantity is first credited per pick, then
     * the shortfall is written off here against the relevant pick's batch so
     * inventory doesn't silently disappear. Records a {@code WASTAGE} movement
     * tagged {@link WastageReason#LOST} so the shrinkage report can categorise
     * it. No-op when {@code quantity <= 0}.
     */
    @Transactional
    public StockBatchDto recordTransitLoss(String pharmacyUid, String medicineUid, String batchNo,
                                           int quantity, String referenceUid, String note) {
        if (quantity <= 0) {
            return null;
        }
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        Medicine medicine = activeMedicine(medicineUid);

        StockBatch batch = batchRepository
                .findByPharmacyUidAndMedicineUidAndBatchNo(pharmacy.getUid(), medicine.getUid(), batchNo)
                .orElseThrow(() -> new NotFoundException(
                        "Batch not found for transit loss: " + batchNo + " @ " + pharmacy.getName()));

        batch.applyDelta(-quantity);
        StockBalance balance = lockOrCreateBalance(pharmacy.getUid(), medicine.getUid());
        balance.applyDelta(-quantity);

        StockMovement movement = recordMovement(balance, batch, StockMovementKind.WASTAGE,
                -quantity, emptyToNull(referenceUid), emptyToNull(note));
        movement.setWastageReason(WastageReason.LOST);
        return toBatchDto(batch, medicine);
    }

    // ----- dispense (prescription) -----------------------------------------

    /**
     * Decrements stock to fulfil an APPROVED prescription, marks it SOLD,
     * and records DISPENSE movement(s) — one per batch consumed. Walks
     * batches in FEFO order; may span multiple batches for a single dispense.
     *
     * <p>{@code salesPharmacyUid} is optional: when set (and different from
     * {@code pharmacyUid}), the prescription is filled at the issuing
     * pharmacy but stock is pulled from the sales pharmacy — the legacy
     * {@code issuePharmacy / salesPharmacy} split. The prescription
     * records both pharmacies for audit.
     */
    @Transactional
    public List<StockMovementDto> dispense(String pharmacyUid, String prescriptionUid, String salesPharmacyUid) {
        Pharmacy issuePharmacy = activePharmacy(pharmacyUid);
        Pharmacy salesPharmacy = (salesPharmacyUid == null || salesPharmacyUid.isBlank()
                || salesPharmacyUid.equals(pharmacyUid))
                ? issuePharmacy
                : activePharmacy(salesPharmacyUid);
        Prescription rx = prescriptionRepository.findByUid(prescriptionUid)
                .orElseThrow(() -> new NotFoundException("Prescription not found: " + prescriptionUid));
        if (rx.getStatus() != PrescriptionStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED prescriptions can be dispensed (current: " + rx.getStatus()
                            + "). Move through accept → verify → approve first.");
        }
        if (rx.getQuantity() == null || rx.getQuantity() <= 0) {
            throw new BusinessRuleException("Prescription has no dispense quantity set");
        }
        Medicine medicine = medicineRepository.findByUid(rx.getMedicineUid())
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + rx.getMedicineUid()));

        List<StockMovement> movements = fefoDecrement(salesPharmacy.getUid(), medicine.getUid(),
                rx.getQuantity(), rx.getUid(),
                "Dispense for " + rx.getPrescriptionNo());

        rx.setIssuePharmacyUid(issuePharmacy.getUid());
        rx.setSalesPharmacyUid(salesPharmacy.getUid());
        rx.markSold();

        return movements.stream().map(m -> toMovementDto(m, salesPharmacy, medicine)).toList();
    }

    // ----- dispense (sale line) --------------------------------------------

    /**
     * Final dispense of an APPROVED retail sale line. Same FEFO mechanics
     * as the prescription path; the sale-order header rolls forward when
     * every line has terminated.
     *
     * <p>{@code salesPharmacyUid} works like the prescription overload —
     * lets stock come from a different pharmacy than the one that opened
     * the sale.
     */
    @Transactional
    public List<StockMovementDto> dispenseSaleLine(String pharmacyUid, String saleLineUid, String salesPharmacyUid) {
        Pharmacy issuePharmacy = activePharmacy(pharmacyUid);
        Pharmacy salesPharmacy = (salesPharmacyUid == null || salesPharmacyUid.isBlank()
                || salesPharmacyUid.equals(pharmacyUid))
                ? issuePharmacy
                : activePharmacy(salesPharmacyUid);
        PharmacySaleOrderLine line = saleLineRepository.findByUid(saleLineUid)
                .orElseThrow(() -> new NotFoundException("Sale line not found: " + saleLineUid));
        if (line.getStatus() != PharmacySaleLineStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED sale lines can be dispensed (current: " + line.getStatus() + ").");
        }
        if (line.getQuantity() <= 0) {
            throw new BusinessRuleException("Sale line has no quantity set");
        }
        PharmacySaleOrder sale = saleRepository.findByUid(line.getSaleUid())
                .orElseThrow(() -> new NotFoundException("Sale not found: " + line.getSaleUid()));
        if (!sale.getPharmacyUid().equals(issuePharmacy.getUid())) {
            throw new BusinessRuleException("Sale was opened at a different pharmacy");
        }
        Medicine medicine = medicineRepository.findByUid(line.getMedicineUid())
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + line.getMedicineUid()));

        List<StockMovement> movements = fefoDecrement(salesPharmacy.getUid(), medicine.getUid(),
                line.getQuantity(), line.getUid(),
                "Sale dispense for " + sale.getSaleNo());

        line.setIssuePharmacyUid(issuePharmacy.getUid());
        line.setSalesPharmacyUid(salesPharmacy.getUid());
        line.markSold();
        long open = saleLineRepository.countBySaleUidAndStatusNotIn(sale.getUid(), TERMINAL_SALE_LINE_STATUSES);
        sale.onLineTransition((int) open);

        return movements.stream().map(m -> toMovementDto(m, salesPharmacy, medicine)).toList();
    }

    /**
     * Walk batches in FEFO order, decrementing each until {@code remaining}
     * is zero. Writes one DISPENSE movement per batch consumed.
     */
    private List<StockMovement> fefoDecrement(String pharmacyUid, String medicineUid,
                                              int requested, String referenceUid, String note) {
        List<StockBatch> batches = batchRepository.lockFefoForDispense(pharmacyUid, medicineUid);
        int onHand = batches.stream().mapToInt(StockBatch::getQuantity).sum();
        if (onHand < requested) {
            throw new BusinessRuleException(
                    "Insufficient stock: on hand " + onHand + ", requested " + requested);
        }

        StockBalance balance = balanceRepository.lockByPharmacyUidAndMedicineUid(pharmacyUid, medicineUid)
                .orElseThrow(() -> new BusinessRuleException("No balance row for medicine"));

        List<StockMovement> movements = new ArrayList<>();
        int remaining = requested;
        for (StockBatch batch : batches) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getQuantity());
            if (take > 0) {
                batch.applyDelta(-take);
                balance.applyDelta(-take);
                movements.add(recordMovement(balance, batch, StockMovementKind.DISPENSE,
                        -take, referenceUid, note));
                remaining -= take;
            }
        }
        if (remaining > 0) {
            // Shouldn't happen — we summed onHand first — but defensive.
            throw new BusinessRuleException("Could not fulfil " + requested + ": " + remaining + " short");
        }
        return movements;
    }

    // ----- read paths -------------------------------------------------------

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int EXPIRY_WINDOW_DAYS = 30;

    /**
     * Server-side, paginated stock list for the UI: filters by medicine
     * name/code, low-stock-only and expiring-only at the DB, then rolls up the
     * page's balance rows with their batch details.
     */
    @Transactional(readOnly = true)
    public PageResponse<StockBalanceDto> searchBalances(String pharmacyUid, String query,
                                                        boolean lowOnly, boolean expiringOnly,
                                                        Pageable pageable) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(pharmacyUid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + pharmacyUid));
        String search = emptyToNull(query);
        LocalDate cutoff = LocalDate.now().plusDays(EXPIRY_WINDOW_DAYS);
        return PageResponse.from(
                balanceRepository.searchBalances(pharmacy.getUid(), search, lowOnly, LOW_STOCK_THRESHOLD,
                        expiringOnly, cutoff, pageable)
                        .map(bal -> toBalanceDto(pharmacy, bal)));
    }

    private StockBalanceDto toBalanceDto(Pharmacy pharmacy, StockBalance bal) {
        Medicine medicine = medicineRepository.findByUid(bal.getMedicineUid()).orElse(null);
        List<StockBatch> rows = batchRepository
                .findAllByPharmacyUidAndMedicineUid(pharmacy.getUid(), bal.getMedicineUid());
        LocalDate earliest = rows.stream().map(StockBatch::getExpiresAt).filter(d -> d != null)
                .min(Comparator.naturalOrder()).orElse(null);
        List<StockBatchDto> batchDtos = rows.stream().map(b -> toBatchDto(b, medicine)).toList();
        return new StockBalanceDto(
                pharmacy.getUid(),
                pharmacy.getName(),
                bal.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getStrength(),
                bal.getQuantity(),
                rows.size(),
                earliest,
                batchDtos);
    }

    /**
     * One row per medicine, with per-batch details rolled up. Used by the
     * stock list page.
     */
    @Transactional(readOnly = true)
    public List<StockBalanceDto> listBalances(String pharmacyUid) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(pharmacyUid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + pharmacyUid));
        List<StockBatch> batches = batchRepository.findAllByPharmacy(pharmacy.getUid());

        // Group batches by medicine, preserving the FEFO-ish order returned
        // by the repository.
        Map<String, List<StockBatch>> byMedicine = new LinkedHashMap<>();
        for (StockBatch b : batches) {
            byMedicine.computeIfAbsent(b.getMedicineUid(), k -> new ArrayList<>()).add(b);
        }

        List<StockBalanceDto> out = new ArrayList<>(byMedicine.size());
        for (Map.Entry<String, List<StockBatch>> entry : byMedicine.entrySet()) {
            Medicine medicine = medicineRepository.findByUid(entry.getKey()).orElse(null);
            List<StockBatch> rows = entry.getValue();
            int total = rows.stream().mapToInt(StockBatch::getQuantity).sum();
            LocalDate earliest = rows.stream()
                    .map(StockBatch::getExpiresAt)
                    .filter(d -> d != null)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            List<StockBatchDto> batchDtos = rows.stream().map(b -> toBatchDto(b, medicine)).toList();
            out.add(new StockBalanceDto(
                    pharmacy.getUid(),
                    pharmacy.getName(),
                    entry.getKey(),
                    medicine == null ? null : medicine.getCode(),
                    medicine == null ? null : medicine.getName(),
                    medicine == null ? null : medicine.getStrength(),
                    total,
                    rows.size(),
                    earliest,
                    batchDtos));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDto> searchMovements(String pharmacyUid, String medicineUid,
                                                          StockMovementKind kind, Pageable pageable) {
        return PageResponse.from(
                movementRepository.search(emptyToNull(pharmacyUid), emptyToNull(medicineUid), kind, pageable)
                        .map(m -> {
                            Pharmacy ph = pharmacyRepository.findByUid(m.getPharmacyUid()).orElse(null);
                            Medicine med = medicineRepository.findByUid(m.getMedicineUid()).orElse(null);
                            return toMovementDto(m, ph, med);
                        }));
    }

    // ----- helpers ----------------------------------------------------------

    private Pharmacy activePharmacy(String uid) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + uid));
        if (!pharmacy.isActive()) {
            throw new BusinessRuleException("Pharmacy is not active: " + pharmacy.getName());
        }
        return pharmacy;
    }

    private Medicine activeMedicine(String uid) {
        Medicine medicine = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!medicine.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + medicine.getName());
        }
        return medicine;
    }

    private StockBalance lockOrCreateBalance(String pharmacyUid, String medicineUid) {
        return balanceRepository.lockByPharmacyUidAndMedicineUid(pharmacyUid, medicineUid)
                .orElseGet(() -> balanceRepository.save(new StockBalance(pharmacyUid, medicineUid)));
    }

    private StockMovement recordMovement(StockBalance balance, StockBatch batch,
                                         StockMovementKind kind, int delta,
                                         String referenceUid, String note) {
        return movementRepository.save(new StockMovement(
                balance.getPharmacyUid(), balance.getMedicineUid(), kind,
                delta, balance.getQuantity(), referenceUid,
                batch == null ? null : batch.getUid(),
                batch == null ? null : batch.getBatchNo(),
                note, currentUsername()));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static StockBatchDto toBatchDto(StockBatch b, Medicine medicine) {
        return new StockBatchDto(
                b.getUid(),
                b.getPharmacyUid(),
                b.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getStrength(),
                b.getBatchNo(),
                b.getExpiresAt(),
                b.isExpired(),
                b.getQuantity(),
                b.getReceivedAt(),
                b.getManufacturedDate());
    }

    private static StockMovementDto toMovementDto(StockMovement m, Pharmacy pharmacy, Medicine medicine) {
        return new StockMovementDto(
                m.getUid(),
                m.getPharmacyUid(),
                pharmacy == null ? null : pharmacy.getName(),
                m.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                m.getBatchUid(),
                m.getBatchNo(),
                m.getKind(),
                m.getQuantity(),
                m.getBalanceAfter(),
                m.getReferenceUid(),
                m.getNote(),
                m.getActorUsername(),
                m.getOccurredAt(),
                m.getCreatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
