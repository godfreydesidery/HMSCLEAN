package com.otapp.hmis.engine.store.stock.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.AdjustStoreStockRequest;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.BatchPickResult;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.ReceiveStoreStockRequest;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.StoreStockBalanceDto;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.StoreStockBatchDto;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.StoreStockMovementDto;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBalance;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBalanceRepository;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBatch;
import com.otapp.hmis.engine.store.stock.domain.StoreStockBatchRepository;
import com.otapp.hmis.engine.store.stock.domain.StoreStockMovement;
import com.otapp.hmis.engine.store.stock.domain.StoreStockMovementKind;
import com.otapp.hmis.engine.store.stock.domain.StoreStockMovementRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
public class StoreStockService {

    private final StoreStockBalanceRepository balanceRepository;
    private final StoreStockBatchRepository batchRepository;
    private final StoreStockMovementRepository movementRepository;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;

    // ----- receive ----------------------------------------------------------

    @Transactional
    public StoreStockBatchDto receive(String storeUid, ReceiveStoreStockRequest request) {
        if (request.quantity() <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        return doReceive(storeUid, request.medicineUid(), request.batchNo(),
                request.expiresAt(), request.quantity(),
                null, emptyToNull(request.note()));
    }

    /**
     * Cross-module entry point used by procurement (goods receipt). The GRN
     * carries the supplier-provided batch number and expiry per line.
     */
    @Transactional
    public StoreStockBatchDto receiveFromProcurement(String storeUid, String medicineUid,
                                                     String batchNo, LocalDate expiresAt,
                                                     int quantity, String referenceUid, String note) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        String effectiveBatch = (batchNo == null || batchNo.isBlank())
                ? "GRN-" + (referenceUid == null ? "UNKNOWN" : referenceUid.substring(0, Math.min(12, referenceUid.length())))
                : batchNo.trim();
        return doReceive(storeUid, medicineUid, effectiveBatch, expiresAt, quantity,
                emptyToNull(referenceUid), emptyToNull(note));
    }

    private StoreStockBatchDto doReceive(String storeUid, String medicineUid, String batchNo,
                                         LocalDate expiresAt, int quantity, String referenceUid, String note) {
        Store store = activeStore(storeUid);
        Medicine medicine = activeMedicine(medicineUid);

        StoreStockBatch batch = batchRepository
                .findByStoreUidAndMedicineUidAndBatchNo(store.getUid(), medicine.getUid(), batchNo)
                .orElseGet(() -> batchRepository.save(
                        new StoreStockBatch(store.getUid(), medicine.getUid(), batchNo, expiresAt)));
        if (expiresAt != null && !expiresAt.equals(batch.getExpiresAt())) {
            batch.setExpiresAt(expiresAt);
        }
        batch.applyDelta(quantity);

        StoreStockBalance balance = lockOrCreateBalance(store.getUid(), medicine.getUid());
        balance.applyDelta(quantity);

        recordMovement(balance, batch, StoreStockMovementKind.RECEIPT, quantity, referenceUid, note);
        return toBatchDto(batch, medicine);
    }

    // ----- issue to pharmacy (cross-module entry) ---------------------------

    /**
     * FEFO-walk the store's batches for {@code medicineUid}, decrementing
     * each in turn until {@code requested} units have been pulled. Returns
     * one {@link BatchPickResult} per batch consumed so the caller (the
     * pharmacy↔store transfer service) can persist matching pick rows and
     * propagate batch metadata to the receiving pharmacy.
     *
     * <p>The destination pharmacy's stock is NOT touched here — that
     * happens later when the pharmacy signs the RN.
     */
    @Transactional
    public List<BatchPickResult> issueToPharmacy(String storeUid, String medicineUid,
                                                 int requested, String referenceUid, String note) {
        if (requested <= 0) {
            throw new BusinessRuleException("Issue quantity must be positive");
        }
        Store store = activeStore(storeUid);
        Medicine medicine = activeMedicine(medicineUid);

        List<StoreStockBatch> batches = batchRepository.lockFefoForIssue(store.getUid(), medicine.getUid());
        int onHand = batches.stream().mapToInt(StoreStockBatch::getQuantity).sum();
        if (onHand < requested) {
            throw new BusinessRuleException(
                    "Insufficient store stock: on hand " + onHand + ", requested " + requested);
        }

        StoreStockBalance balance = balanceRepository.lockByStoreUidAndMedicineUid(store.getUid(), medicine.getUid())
                .orElseThrow(() -> new BusinessRuleException("No balance row for medicine"));

        List<BatchPickResult> picks = new ArrayList<>();
        int remaining = requested;
        for (StoreStockBatch batch : batches) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getQuantity());
            if (take > 0) {
                batch.applyDelta(-take);
                balance.applyDelta(-take);
                StoreStockMovement movement = recordMovement(balance, batch,
                        StoreStockMovementKind.ISSUE, -take,
                        emptyToNull(referenceUid), emptyToNull(note));
                picks.add(new BatchPickResult(
                        batch.getUid(), batch.getBatchNo(), batch.getExpiresAt(),
                        take, movement.getUid()));
                remaining -= take;
            }
        }
        if (remaining > 0) {
            throw new BusinessRuleException("Could not fulfil " + requested + ": " + remaining + " short");
        }
        return picks;
    }

    // ----- adjust -----------------------------------------------------------

    @Transactional
    public StoreStockBatchDto adjust(String storeUid, AdjustStoreStockRequest request) {
        Store store = activeStore(storeUid);
        if (request.delta() == 0) {
            throw new BusinessRuleException("Adjustment delta must be non-zero");
        }
        StoreStockBatch batch = batchRepository.findByUid(request.batchUid())
                .orElseThrow(() -> new NotFoundException("Batch not found: " + request.batchUid()));
        if (!batch.getStoreUid().equals(store.getUid())) {
            throw new BusinessRuleException("Batch belongs to a different store");
        }
        Medicine medicine = activeMedicine(batch.getMedicineUid());

        batch.applyDelta(request.delta());
        StoreStockBalance balance = lockOrCreateBalance(store.getUid(), medicine.getUid());
        balance.applyDelta(request.delta());

        recordMovement(balance, batch, StoreStockMovementKind.ADJUSTMENT, request.delta(),
                null, emptyToNull(request.note()));
        return toBatchDto(batch, medicine);
    }

    // ----- read paths -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<StoreStockBalanceDto> listBalances(String storeUid) {
        Store store = storeRepository.findByUid(storeUid)
                .orElseThrow(() -> new NotFoundException("Store not found: " + storeUid));
        List<StoreStockBatch> batches = batchRepository.findAllByStore(store.getUid());

        Map<String, List<StoreStockBatch>> byMedicine = new LinkedHashMap<>();
        for (StoreStockBatch b : batches) {
            byMedicine.computeIfAbsent(b.getMedicineUid(), k -> new ArrayList<>()).add(b);
        }

        List<StoreStockBalanceDto> out = new ArrayList<>(byMedicine.size());
        for (Map.Entry<String, List<StoreStockBatch>> entry : byMedicine.entrySet()) {
            Medicine medicine = medicineRepository.findByUid(entry.getKey()).orElse(null);
            List<StoreStockBatch> rows = entry.getValue();
            int total = rows.stream().mapToInt(StoreStockBatch::getQuantity).sum();
            LocalDate earliest = rows.stream()
                    .map(StoreStockBatch::getExpiresAt)
                    .filter(d -> d != null)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            List<StoreStockBatchDto> batchDtos = rows.stream().map(b -> toBatchDto(b, medicine)).toList();
            out.add(new StoreStockBalanceDto(
                    store.getUid(),
                    store.getName(),
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
    public PageResponse<StoreStockMovementDto> searchMovements(String storeUid, String medicineUid,
                                                               StoreStockMovementKind kind, Pageable pageable) {
        return PageResponse.from(
                movementRepository.search(emptyToNull(storeUid), emptyToNull(medicineUid), kind, pageable)
                        .map(m -> {
                            Store store = storeRepository.findByUid(m.getStoreUid()).orElse(null);
                            Medicine med = medicineRepository.findByUid(m.getMedicineUid()).orElse(null);
                            return toMovementDto(m, store, med);
                        }));
    }

    // ----- helpers ----------------------------------------------------------

    private Store activeStore(String uid) {
        Store store = storeRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Store not found: " + uid));
        if (!store.isActive()) {
            throw new BusinessRuleException("Store is not active: " + store.getName());
        }
        return store;
    }

    private Medicine activeMedicine(String uid) {
        Medicine medicine = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!medicine.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + medicine.getName());
        }
        return medicine;
    }

    private StoreStockBalance lockOrCreateBalance(String storeUid, String medicineUid) {
        return balanceRepository.lockByStoreUidAndMedicineUid(storeUid, medicineUid)
                .orElseGet(() -> balanceRepository.save(new StoreStockBalance(storeUid, medicineUid)));
    }

    private StoreStockMovement recordMovement(StoreStockBalance balance, StoreStockBatch batch,
                                              StoreStockMovementKind kind, int delta,
                                              String referenceUid, String note) {
        return movementRepository.save(new StoreStockMovement(
                balance.getStoreUid(), balance.getMedicineUid(), kind,
                delta, balance.getQuantity(), referenceUid,
                batch == null ? null : batch.getUid(),
                batch == null ? null : batch.getBatchNo(),
                note, currentUsername()));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static StoreStockBatchDto toBatchDto(StoreStockBatch b, Medicine medicine) {
        return new StoreStockBatchDto(
                b.getUid(),
                b.getStoreUid(),
                b.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getStrength(),
                b.getBatchNo(),
                b.getExpiresAt(),
                b.isExpired(),
                b.getQuantity(),
                b.getReceivedAt());
    }

    private static StoreStockMovementDto toMovementDto(StoreStockMovement m, Store store, Medicine medicine) {
        return new StoreStockMovementDto(
                m.getUid(),
                m.getStoreUid(),
                store == null ? null : store.getName(),
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
