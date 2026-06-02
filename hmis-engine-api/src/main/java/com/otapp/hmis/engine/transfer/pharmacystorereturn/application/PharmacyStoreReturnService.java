package com.otapp.hmis.engine.transfer.pharmacystorereturn.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.application.UnitConversionService;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnit;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnitRepository;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.masterdata.store.application.StoreStaffService;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.BatchPickResult;
import com.otapp.hmis.engine.pharmacy.stock.application.StockService;
import com.otapp.hmis.engine.store.stock.application.StoreStockService;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.BatchPickDto;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.CreateReturnLineRequest;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.CreateReturnRequest;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReasonRequest;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReturnDto;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReturnLineDto;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.application.PharmacyStoreReturnDtos.ReturnSummary;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturn;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnBatchPick;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnBatchPickRepository;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnLine;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnLineRepository;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnRepository;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.domain.PharmacyStoreReturnStatus;
import com.otapp.hmis.engine.transfer.pharmacystorereturn.infrastructure.PharmacyStoreReturnNumberGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pharmacy → store returns (PROCESS.md §8.5 reverse direction). DRAFT
 * → SUBMITTED → COMPLETED with REJECTED / CANCELLED branches. On
 * COMPLETED the pharmacy is debited FEFO via TRANSFER_OUT and the store
 * is credited per source batch via the RETURN movement kind.
 */
@Service
@RequiredArgsConstructor
public class PharmacyStoreReturnService {

    private final PharmacyStoreReturnRepository returnRepository;
    private final PharmacyStoreReturnLineRepository lineRepository;
    private final PharmacyStoreReturnBatchPickRepository pickRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final UnitConversionService unitConversionService;
    private final StockService pharmacyStockService;
    private final StoreStockService storeStockService;
    private final StoreStaffService storeStaffService;
    private final PharmacyStoreReturnNumberGenerator numberGenerator;

    @Transactional
    public ReturnDto create(CreateReturnRequest request) {
        Pharmacy pharmacy = activePharmacy(request.pharmacyUid());
        Store store = activeStore(request.storeUid());

        PharmacyStoreReturn r = returnRepository.save(new PharmacyStoreReturn(
                numberGenerator.next(),
                pharmacy.getUid(),
                store.getUid(),
                request.returnDate(),
                emptyToNull(request.reason()),
                emptyToNull(request.note())));

        for (CreateReturnLineRequest line : request.lines()) {
            Medicine medicine = activeMedicine(line.medicineUid());
            MedicineUnit unit = unitConversionService.resolveUnit(medicine.getUid(), emptyToNull(line.unitUid()));
            int baseQty = unitConversionService.toBaseQuantity(unit, line.quantity());
            lineRepository.save(new PharmacyStoreReturnLine(
                    r.getUid(), medicine.getUid(), unit.getUid(),
                    baseQty, emptyToNull(line.reason())));
        }
        return toDto(r);
    }

    @Transactional
    public ReturnDto submit(String returnUid) {
        PharmacyStoreReturn r = loadOrThrow(returnUid);
        if (lineRepository.findAllByReturnUidOrderByCreatedAtAsc(r.getUid()).isEmpty()) {
            throw new BusinessRuleException("Cannot submit a return with no lines");
        }
        r.submit(currentUsername());
        return toDto(r);
    }

    /**
     * Approve + apply stock effects atomically. Walks pharmacy batches
     * FEFO per line; the resulting picks are persisted and credited to
     * the store with the {@code RETURN} movement kind.
     */
    @Transactional
    public ReturnDto complete(String returnUid) {
        PharmacyStoreReturn r = loadOrThrow(returnUid);
        requireStoreMembership(r.getStoreUid());
        if (r.getStatus() != PharmacyStoreReturnStatus.SUBMITTED) {
            throw new BusinessRuleException(
                    "Only SUBMITTED returns can be completed (current: " + r.getStatus() + ")");
        }

        List<PharmacyStoreReturnLine> lines = lineRepository.findAllByReturnUidOrderByCreatedAtAsc(r.getUid());
        for (PharmacyStoreReturnLine line : lines) {
            List<BatchPickResult> picks = pharmacyStockService.issueToPharmacy(
                    r.getPharmacyUid(),
                    line.getMedicineUid(),
                    line.getQuantity(),
                    r.getUid(),
                    "Return to store " + r.getReturnNo());

            for (BatchPickResult pick : picks) {
                pickRepository.save(new PharmacyStoreReturnBatchPick(
                        line.getUid(),
                        pick.batchUid(), pick.batchNo(), pick.manufacturedDate(), pick.expiresAt(),
                        pick.quantity()));
                storeStockService.receiveFromPharmacyReturn(
                        r.getStoreUid(),
                        line.getMedicineUid(),
                        pick.batchNo(), pick.manufacturedDate(), pick.expiresAt(),
                        pick.quantity(),
                        r.getUid(),
                        "Return from pharmacy " + r.getPharmacyUid() + " — " + r.getReturnNo());
            }
        }
        r.complete(currentUsername());
        return toDto(r);
    }

    @Transactional
    public ReturnDto reject(String returnUid, ReasonRequest request) {
        PharmacyStoreReturn r = loadOrThrow(returnUid);
        r.reject(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        return toDto(r);
    }

    @Transactional
    public ReturnDto cancel(String returnUid) {
        PharmacyStoreReturn r = loadOrThrow(returnUid);
        r.cancel();
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public ReturnDto findByUid(String returnUid) {
        return toDto(loadOrThrow(returnUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnSummary> search(String query, PharmacyStoreReturnStatus status,
                                              String pharmacyUid, String storeUid, Pageable pageable) {
        return PageResponse.from(
                returnRepository.search(emptyToNull(query), status,
                                emptyToNull(pharmacyUid), emptyToNull(storeUid), pageable)
                        .map(this::toSummary));
    }

    // ----- helpers ---------------------------------------------------------

    private PharmacyStoreReturn loadOrThrow(String uid) {
        return returnRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Return not found: " + uid));
    }

    private Pharmacy activePharmacy(String uid) {
        Pharmacy p = pharmacyRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + uid));
        if (!p.isActive()) {
            throw new BusinessRuleException("Pharmacy is not active: " + p.getName());
        }
        return p;
    }

    private Store activeStore(String uid) {
        Store s = storeRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Store not found: " + uid));
        if (!s.isActive()) {
            throw new BusinessRuleException("Store is not active: " + s.getName());
        }
        return s;
    }

    private Medicine activeMedicine(String uid) {
        Medicine m = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!m.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + m.getName());
        }
        return m;
    }

    private ReturnDto toDto(PharmacyStoreReturn r) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(r.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(r.getStoreUid()).orElse(null);
        List<PharmacyStoreReturnLine> lines = lineRepository.findAllByReturnUidOrderByCreatedAtAsc(r.getUid());
        return new ReturnDto(
                r.getUid(), r.getReturnNo(),
                r.getPharmacyUid(), pharmacy == null ? null : pharmacy.getName(),
                r.getStoreUid(),    store    == null ? null : store.getName(),
                r.getReturnDate(), r.getReason(), r.getNote(), r.getStatus(),
                r.getSubmittedAt(),  r.getSubmittedByUsername(),
                r.getCompletedAt(),  r.getCompletedByUsername(),
                r.getRejectedAt(),   r.getRejectedByUsername(), r.getRejectReason(),
                r.getCancelledAt(),
                r.getCreatedAt(), r.getUpdatedAt(),
                lines.stream().map(this::toLineDto).toList());
    }

    private ReturnLineDto toLineDto(PharmacyStoreReturnLine line) {
        Medicine m = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        MedicineUnit unit = lookupUnit(line.getUnitUid());
        List<PharmacyStoreReturnBatchPick> picks = pickRepository
                .findAllByReturnLineUidOrderByCreatedAtAsc(line.getUid());
        List<BatchPickDto> pickDtos = new ArrayList<>(picks.size());
        for (PharmacyStoreReturnBatchPick p : picks) {
            pickDtos.add(new BatchPickDto(p.getSourceBatchUid(), p.getBatchNo(),
                    p.getManufacturedDate(), p.getExpiresAt(), p.getQuantity()));
        }
        return new ReturnLineDto(
                line.getUid(),
                line.getMedicineUid(),
                m == null ? null : m.getCode(),
                m == null ? null : m.getName(),
                m == null ? null : m.getStrength(),
                line.getUnitUid(),
                unit == null ? null : unit.getCode(),
                unit == null ? 1 : unit.getFactorToBase(),
                line.getQuantity(),
                line.getReason(),
                pickDtos,
                line.getCreatedAt());
    }

    private ReturnSummary toSummary(PharmacyStoreReturn r) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(r.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(r.getStoreUid()).orElse(null);
        int lineCount = lineRepository.findAllByReturnUidOrderByCreatedAtAsc(r.getUid()).size();
        return new ReturnSummary(
                r.getUid(), r.getReturnNo(),
                pharmacy == null ? null : pharmacy.getName(),
                store    == null ? null : store.getName(),
                r.getReturnDate(), r.getStatus(),
                lineCount, r.getCreatedAt());
    }

    private MedicineUnit lookupUnit(String unitUid) {
        if (unitUid == null || unitUid.isBlank()) return null;
        return medicineUnitRepository.findByUid(unitUid).orElse(null);
    }

    /**
     * Legacy fidelity gate: only a store keeper affiliated with the
     * destination store ({@code StorePerson.stores}) may complete a return
     * into it — the coarse PHARMACY_ACCESS/STORE_ACCESS authority isn't
     * enough on its own.
     */
    private void requireStoreMembership(String storeUid) {
        String username = currentUsername();
        if (!storeStaffService.isAssigned(storeUid, username)) {
            throw new BusinessRuleException(
                    "User " + username + " is not assigned to store " + storeUid);
        }
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
