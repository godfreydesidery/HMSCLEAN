package com.otapp.hmis.engine.transfer.pharmacystore.application;

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
import com.otapp.hmis.engine.pharmacy.stock.application.StockService;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.BatchPickResult;
import com.otapp.hmis.engine.store.stock.application.StoreStockService;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateROLineRequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateRORequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateRNLineRequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateRNRequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateTOLineRequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.CreateTORequest;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.RNDto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.RNLineDto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.RNSummary;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.RODto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.ROLineDto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.ROSummary;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.TOBatchPickDto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.TODto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.TOLineDto;
import com.otapp.hmis.engine.transfer.pharmacystore.application.PharmacyStoreTransferDtos.TOSummary;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.PharmacyToStoreRO;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.PharmacyToStoreROLine;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.PharmacyToStoreROLineRepository;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.PharmacyToStoreRORepository;
import com.otapp.hmis.engine.transfer.common.domain.ReceiveNoteStatus;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyRN;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyRNLine;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyRNLineRepository;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyRNRepository;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyTO;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyTOBatchPick;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyTOBatchPickRepository;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyTOLine;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyTOLineRepository;
import com.otapp.hmis.engine.transfer.pharmacystore.domain.StoreToPharmacyTORepository;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import com.otapp.hmis.engine.transfer.pharmacystore.infrastructure.PharmacyToStoreRONumberGenerator;
import com.otapp.hmis.engine.transfer.pharmacystore.infrastructure.StoreToPharmacyRNNumberGenerator;
import com.otapp.hmis.engine.transfer.pharmacystore.infrastructure.StoreToPharmacyTONumberGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the three-document pharmacy ↔ store transfer chain
 * (PROCESS.md §8.5). Lifecycle: a pharmacy raises an RO; once APPROVED +
 * SUBMITTED the store creates a TO against it; issuing the TO decrements
 * store stock FEFO; the receiving pharmacy then files an RN that
 * increments pharmacy stock per source batch and closes the chain.
 */
@Service
@RequiredArgsConstructor
public class PharmacyStoreTransferService {

    private final PharmacyToStoreRORepository roRepository;
    private final PharmacyToStoreROLineRepository roLineRepository;
    private final StoreToPharmacyTORepository toRepository;
    private final StoreToPharmacyTOLineRepository toLineRepository;
    private final StoreToPharmacyTOBatchPickRepository toPickRepository;
    private final StoreToPharmacyRNRepository rnRepository;
    private final StoreToPharmacyRNLineRepository rnLineRepository;

    private final PharmacyRepository pharmacyRepository;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final UnitConversionService unitConversionService;

    private final StoreStockService storeStockService;
    private final StockService pharmacyStockService;
    private final StoreStaffService storeStaffService;

    private final PharmacyToStoreRONumberGenerator roNumberGenerator;
    private final StoreToPharmacyTONumberGenerator toNumberGenerator;
    private final StoreToPharmacyRNNumberGenerator rnNumberGenerator;

    // ========================================================================
    // RO operations
    // ========================================================================

    @Transactional
    public RODto createRO(CreateRORequest request) {
        Pharmacy pharmacy = activePharmacy(request.pharmacyUid());
        Store store = activeStore(request.storeUid());

        PharmacyToStoreRO ro = roRepository.save(new PharmacyToStoreRO(
                roNumberGenerator.next(),
                pharmacy.getUid(),
                store.getUid(),
                null,
                request.validUntil(),
                emptyToNull(request.note())));

        for (CreateROLineRequest line : request.lines()) {
            Medicine medicine = activeMedicine(line.medicineUid());
            MedicineUnit unit = unitConversionService.resolveUnit(medicine.getUid(), emptyToNull(line.unitUid()));
            int baseQty = unitConversionService.toBaseQuantity(unit, line.quantity());
            roLineRepository.save(new PharmacyToStoreROLine(
                    ro.getUid(), medicine.getUid(), unit.getUid(), baseQty, emptyToNull(line.note())));
        }
        return toRODto(ro);
    }

    @Transactional
    public RODto verifyRO(String roUid) {
        PharmacyToStoreRO ro = loadRO(roUid);
        ro.verify();
        return toRODto(ro);
    }

    @Transactional
    public RODto approveRO(String roUid) {
        PharmacyToStoreRO ro = loadRO(roUid);
        ro.approve();
        return toRODto(ro);
    }

    @Transactional
    public RODto submitRO(String roUid) {
        PharmacyToStoreRO ro = loadRO(roUid);
        ro.submit();
        return toRODto(ro);
    }

    @Transactional
    public RODto rejectRO(String roUid, String reason) {
        PharmacyToStoreRO ro = loadRO(roUid);
        ro.reject(emptyToNull(reason));
        return toRODto(ro);
    }

    @Transactional
    public RODto returnRO(String roUid, String reason) {
        PharmacyToStoreRO ro = loadRO(roUid);
        ro.returnToRequester(emptyToNull(reason));
        return toRODto(ro);
    }

    @Transactional(readOnly = true)
    public RODto findRO(String roUid) {
        return toRODto(loadRO(roUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<ROSummary> searchROs(String query, TransferDocStatus status,
                                             String pharmacyUid, String storeUid, Pageable pageable) {
        return PageResponse.from(
                roRepository.search(emptyToNull(query), status,
                                emptyToNull(pharmacyUid), emptyToNull(storeUid), pageable)
                        .map(this::toROSummary));
    }

    // ========================================================================
    // TO operations
    // ========================================================================

    @Transactional
    public TODto createTO(CreateTORequest request) {
        PharmacyToStoreRO ro = loadRO(request.roUid());
        if (ro.getStatus() != TransferDocStatus.SUBMITTED
                && ro.getStatus() != TransferDocStatus.IN_PROCESS) {
            throw new BusinessRuleException(
                    "TO can only be created against a SUBMITTED or IN_PROCESS RO (current: "
                            + ro.getStatus() + ")");
        }

        StoreToPharmacyTO to = toRepository.save(new StoreToPharmacyTO(
                toNumberGenerator.next(),
                ro.getUid(),
                ro.getPharmacyUid(),
                ro.getStoreUid(),
                null,
                emptyToNull(request.note())));

        for (CreateTOLineRequest line : request.lines()) {
            PharmacyToStoreROLine roLine = roLineRepository.findByUid(line.roLineUid())
                    .orElseThrow(() -> new NotFoundException("RO line not found: " + line.roLineUid()));
            if (!roLine.getRoUid().equals(ro.getUid())) {
                throw new BusinessRuleException("RO line does not belong to this RO");
            }
            MedicineUnit unit = unitConversionService.resolveUnit(roLine.getMedicineUid(), roLine.getUnitUid());
            int baseQty = unitConversionService.toBaseQuantity(unit, line.quantity());
            if (baseQty > roLine.outstandingQuantity()) {
                throw new BusinessRuleException(
                        "TO line quantity " + line.quantity() + " " + unit.getCode()
                                + " (= " + baseQty + " base) exceeds RO outstanding "
                                + roLine.outstandingQuantity() + " base for medicine " + roLine.getMedicineUid());
            }
            toLineRepository.save(new StoreToPharmacyTOLine(
                    to.getUid(), roLine.getUid(), roLine.getMedicineUid(), unit.getUid(), baseQty));
        }

        ro.markInProcess();
        return toTODto(to);
    }

    @Transactional
    public TODto verifyTO(String toUid) {
        StoreToPharmacyTO to = loadTO(toUid);
        to.verify();
        return toTODto(to);
    }

    @Transactional
    public TODto approveTO(String toUid) {
        StoreToPharmacyTO to = loadTO(toUid);
        to.approve();
        return toTODto(to);
    }

    @Transactional
    public TODto rejectTO(String toUid, String reason) {
        StoreToPharmacyTO to = loadTO(toUid);
        to.reject(emptyToNull(reason));
        return toTODto(to);
    }

    /**
     * Decrements store stock FEFO per TO line, persists the resulting batch
     * picks against the line, rolls fulfilment forward on the parent RO,
     * and advances both documents to GOODS_ISSUED.
     */
    @Transactional
    public TODto issueTO(String toUid) {
        StoreToPharmacyTO to = loadTO(toUid);
        requireStoreMembership(to.getStoreUid());
        PharmacyToStoreRO ro = loadRO(to.getRoUid());

        List<StoreToPharmacyTOLine> lines = toLineRepository.findAllByToUidOrderByCreatedAtAsc(to.getUid());
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Cannot issue a TO with no lines");
        }
        for (StoreToPharmacyTOLine line : lines) {
            int outstanding = line.getRequestedQuantity() - line.getIssuedQuantity();
            if (outstanding <= 0) continue;

            List<BatchPickResult> picks = storeStockService.issueToPharmacy(
                    to.getStoreUid(), line.getMedicineUid(), outstanding,
                    to.getUid(), "Issue for TO " + to.getToNo());

            for (BatchPickResult pick : picks) {
                toPickRepository.save(new StoreToPharmacyTOBatchPick(
                        line.getUid(),
                        pick.batchUid(),
                        pick.batchNo(),
                        pick.manufacturedDate(),
                        pick.expiresAt(),
                        pick.quantity()));
            }
            line.recordIssue(outstanding);

            PharmacyToStoreROLine roLine = roLineRepository.findByUid(line.getRoLineUid())
                    .orElseThrow(() -> new NotFoundException("RO line not found: " + line.getRoLineUid()));
            roLine.recordFulfilment(outstanding);
        }

        to.markGoodsIssued();
        ro.markGoodsIssued();
        return toTODto(to);
    }

    @Transactional(readOnly = true)
    public TODto findTO(String toUid) {
        return toTODto(loadTO(toUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<TOSummary> searchTOs(String query, TransferDocStatus status,
                                             String pharmacyUid, String storeUid,
                                             String roUid, Pageable pageable) {
        return PageResponse.from(
                toRepository.search(emptyToNull(query), status,
                                emptyToNull(pharmacyUid), emptyToNull(storeUid),
                                emptyToNull(roUid), pageable)
                        .map(this::toTOSummary));
    }

    // ========================================================================
    // RN operations
    // ========================================================================

    /**
     * Pharmacy confirms receipt. Walks the TO's picks for each line: each
     * pick contributes its full quantity to pharmacy stock if the line's
     * receivedQuantity covers it; remaining shortfall on the line is left
     * uncredited (transit loss). Marks RN COMPLETED and rolls TO + RO to
     * COMPLETED.
     */
    @Transactional
    public RNDto createRN(CreateRNRequest request) {
        StoreToPharmacyTO to = loadTO(request.toUid());
        PharmacyToStoreRO ro = loadRO(to.getRoUid());

        if (to.getStatus() != TransferDocStatus.GOODS_ISSUED) {
            throw new BusinessRuleException(
                    "RN can only be filed against a GOODS_ISSUED TO (current: " + to.getStatus() + ")");
        }
        if (rnRepository.existsByToUidAndStatus(to.getUid(), ReceiveNoteStatus.COMPLETED)) {
            throw new BusinessRuleException("TO has already been received");
        }

        StoreToPharmacyRN rn = rnRepository.save(new StoreToPharmacyRN(
                rnNumberGenerator.next(),
                to.getUid(),
                to.getPharmacyUid(),
                to.getStoreUid(),
                request.receivingDate(),
                emptyToNull(request.note())));

        for (CreateRNLineRequest lineReq : request.lines()) {
            StoreToPharmacyTOLine toLine = toLineRepository.findByUid(lineReq.toLineUid())
                    .orElseThrow(() -> new NotFoundException("TO line not found: " + lineReq.toLineUid()));
            if (!toLine.getToUid().equals(to.getUid())) {
                throw new BusinessRuleException("TO line does not belong to this TO");
            }
            MedicineUnit unit = unitConversionService.resolveUnit(toLine.getMedicineUid(), toLine.getUnitUid());
            int receivedBase = lineReq.receivedQuantity() == 0
                    ? 0
                    : unitConversionService.toBaseQuantity(unit, lineReq.receivedQuantity());
            if (receivedBase > toLine.getIssuedQuantity()) {
                throw new BusinessRuleException(
                        "Received qty " + lineReq.receivedQuantity() + " " + unit.getCode()
                                + " (= " + receivedBase + " base) exceeds issued "
                                + toLine.getIssuedQuantity() + " base for medicine "
                                + toLine.getMedicineUid());
            }
            if (toLine.getReceivedQuantity() > 0) {
                throw new BusinessRuleException(
                        "TO line " + toLine.getUid() + " has already been received");
            }

            StoreToPharmacyRNLine rnLine = rnLineRepository.save(new StoreToPharmacyRNLine(
                    rn.getUid(),
                    toLine.getUid(),
                    toLine.getMedicineUid(),
                    unit.getUid(),
                    toLine.getIssuedQuantity(),
                    receivedBase));

            if (receivedBase > 0) {
                allocateAcrossPicks(rn, to, toLine, rnLine, receivedBase);
                toLine.recordReceipt(receivedBase);
            }
        }

        rn.markCompleted();
        to.markCompleted();
        ro.markCompleted();
        return toRNDto(rn);
    }

    /**
     * Credits every pick its FULL quantity (sum == issued) to the receiving
     * pharmacy, then — if the RN short-received — writes off the shortfall as
     * a transit loss at the receiving pharmacy. The write-off walks the picks
     * in REVERSE so the loss is attributed to the latest-expiry tail batches.
     * Net effect on the destination balance is exactly {@code received}: a
     * TRANSFER_IN of {@code issued} plus a WASTAGE/LOST of the shortfall.
     */
    private void allocateAcrossPicks(StoreToPharmacyRN rn, StoreToPharmacyTO to,
                                     StoreToPharmacyTOLine toLine,
                                     StoreToPharmacyRNLine rnLine, int received) {
        List<StoreToPharmacyTOBatchPick> picks = toPickRepository
                .findAllByToLineUidOrderByCreatedAtAsc(toLine.getUid());

        // Credit each pick its full issued quantity to pharmacy stock.
        for (StoreToPharmacyTOBatchPick pick : picks) {
            pharmacyStockService.receiveFromStore(
                    to.getPharmacyUid(),
                    toLine.getMedicineUid(),
                    pick.getBatchNo(),
                    pick.getManufacturedDate(),
                    pick.getExpiresAt(),
                    pick.getQuantity(),
                    to.getUid(),
                    "RN " + rnLine.getRnUid() + " pick " + pick.getUid());
            pick.setRnLineUid(rnLine.getUid());
        }

        // Write off any shortfall as a transit loss, walking picks in reverse
        // so the latest-expiry tail batches absorb the loss first.
        int shortfall = toLine.getIssuedQuantity() - received;
        int remaining = shortfall;
        for (int i = picks.size() - 1; i >= 0 && remaining > 0; i--) {
            StoreToPharmacyTOBatchPick pick = picks.get(i);
            int loss = Math.min(remaining, pick.getQuantity());
            pharmacyStockService.recordTransitLoss(
                    to.getPharmacyUid(),
                    toLine.getMedicineUid(),
                    pick.getBatchNo(),
                    loss,
                    to.getUid(),
                    "Transit loss on RN " + rn.getRnNo() + " TO " + to.getToNo());
            remaining -= loss;
        }
        // Defensive only: unreachable while sum(picks) == issuedQuantity (the
        // issue path fully allocates), so shortfall can never exceed the picks.
        // Kept as a tripwire against future data corruption.
        if (remaining > 0) {
            throw new BusinessRuleException(
                    "Shortfall exceeds sum of TO picks — TO data is inconsistent");
        }
    }

    @Transactional(readOnly = true)
    public RNDto findRN(String rnUid) {
        return toRNDto(loadRN(rnUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<RNSummary> searchRNs(String query, ReceiveNoteStatus status,
                                             String pharmacyUid, String storeUid,
                                             String toUid, Pageable pageable) {
        return PageResponse.from(
                rnRepository.search(emptyToNull(query), status,
                                emptyToNull(pharmacyUid), emptyToNull(storeUid),
                                emptyToNull(toUid), pageable)
                        .map(this::toRNSummary));
    }

    // ========================================================================
    // Loaders + mapping
    // ========================================================================

    /**
     * Legacy fidelity gate: only a store keeper affiliated with the source
     * store may issue goods out of it ({@code StorePerson.stores}).
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

    private PharmacyToStoreRO loadRO(String uid) {
        return roRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("RO not found: " + uid));
    }

    private StoreToPharmacyTO loadTO(String uid) {
        return toRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("TO not found: " + uid));
    }

    private StoreToPharmacyRN loadRN(String uid) {
        return rnRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("RN not found: " + uid));
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

    private RODto toRODto(PharmacyToStoreRO ro) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(ro.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(ro.getStoreUid()).orElse(null);
        List<PharmacyToStoreROLine> lines = roLineRepository.findAllByRoUidOrderByCreatedAtAsc(ro.getUid());
        return new RODto(
                ro.getUid(),
                ro.getRoNo(),
                ro.getPharmacyUid(), pharmacy == null ? null : pharmacy.getName(),
                ro.getStoreUid(), store == null ? null : store.getName(),
                ro.getOrderDate(), ro.getValidUntil(),
                ro.getStatus(),
                ro.getVerifiedAt(), ro.getApprovedAt(), ro.getSubmittedAt(),
                ro.getInProcessAt(), ro.getIssuedAt(), ro.getCompletedAt(),
                ro.getRejectedAt(), ro.getReturnedAt(),
                ro.getRejectReason(), ro.getNote(),
                ro.getCreatedAt(), ro.getUpdatedAt(),
                lines.stream().map(this::toROLineDto).toList());
    }

    private ROLineDto toROLineDto(PharmacyToStoreROLine line) {
        Medicine m = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        MedicineUnit unit = lookupUnit(line.getUnitUid());
        return new ROLineDto(
                line.getUid(),
                line.getMedicineUid(),
                m == null ? null : m.getCode(),
                m == null ? null : m.getName(),
                m == null ? null : m.getStrength(),
                line.getUnitUid(),
                unit == null ? null : unit.getCode(),
                unit == null ? 1 : unit.getFactorToBase(),
                line.getRequestedQuantity(),
                line.getFulfilledQuantity(),
                line.outstandingQuantity(),
                line.getNote(),
                line.getCreatedAt());
    }

    private ROSummary toROSummary(PharmacyToStoreRO ro) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(ro.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(ro.getStoreUid()).orElse(null);
        int lineCount = roLineRepository.findAllByRoUidOrderByCreatedAtAsc(ro.getUid()).size();
        return new ROSummary(
                ro.getUid(),
                ro.getRoNo(),
                pharmacy == null ? null : pharmacy.getName(),
                store == null ? null : store.getName(),
                ro.getOrderDate(),
                ro.getStatus(),
                lineCount,
                ro.getCreatedAt());
    }

    private TODto toTODto(StoreToPharmacyTO to) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(to.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(to.getStoreUid()).orElse(null);
        PharmacyToStoreRO ro = roRepository.findByUid(to.getRoUid()).orElse(null);
        List<StoreToPharmacyTOLine> lines = toLineRepository.findAllByToUidOrderByCreatedAtAsc(to.getUid());
        return new TODto(
                to.getUid(),
                to.getToNo(),
                to.getRoUid(), ro == null ? null : ro.getRoNo(),
                to.getPharmacyUid(), pharmacy == null ? null : pharmacy.getName(),
                to.getStoreUid(), store == null ? null : store.getName(),
                to.getOrderDate(),
                to.getStatus(),
                to.getVerifiedAt(), to.getApprovedAt(), to.getIssuedAt(),
                to.getCompletedAt(), to.getRejectedAt(),
                to.getRejectedReason(), to.getNote(),
                to.getCreatedAt(), to.getUpdatedAt(),
                lines.stream().map(this::toTOLineDto).toList());
    }

    private TOLineDto toTOLineDto(StoreToPharmacyTOLine line) {
        Medicine m = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        MedicineUnit unit = lookupUnit(line.getUnitUid());
        List<StoreToPharmacyTOBatchPick> picks = toPickRepository
                .findAllByToLineUidOrderByCreatedAtAsc(line.getUid());
        List<TOBatchPickDto> pickDtos = new ArrayList<>(picks.size());
        for (StoreToPharmacyTOBatchPick p : picks) {
            pickDtos.add(new TOBatchPickDto(
                    p.getSourceBatchUid(), p.getBatchNo(), p.getManufacturedDate(),
                    p.getExpiresAt(), p.getQuantity(), p.getRnLineUid()));
        }
        return new TOLineDto(
                line.getUid(),
                line.getRoLineUid(),
                line.getMedicineUid(),
                m == null ? null : m.getCode(),
                m == null ? null : m.getName(),
                m == null ? null : m.getStrength(),
                line.getUnitUid(),
                unit == null ? null : unit.getCode(),
                unit == null ? 1 : unit.getFactorToBase(),
                line.getRequestedQuantity(),
                line.getIssuedQuantity(),
                line.getReceivedQuantity(),
                pickDtos,
                line.getCreatedAt());
    }

    private TOSummary toTOSummary(StoreToPharmacyTO to) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(to.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(to.getStoreUid()).orElse(null);
        PharmacyToStoreRO ro = roRepository.findByUid(to.getRoUid()).orElse(null);
        int lineCount = toLineRepository.findAllByToUidOrderByCreatedAtAsc(to.getUid()).size();
        return new TOSummary(
                to.getUid(),
                to.getToNo(),
                ro == null ? null : ro.getRoNo(),
                pharmacy == null ? null : pharmacy.getName(),
                store == null ? null : store.getName(),
                to.getOrderDate(),
                to.getStatus(),
                lineCount,
                to.getCreatedAt());
    }

    private RNDto toRNDto(StoreToPharmacyRN rn) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(rn.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(rn.getStoreUid()).orElse(null);
        StoreToPharmacyTO to = toRepository.findByUid(rn.getToUid()).orElse(null);
        List<StoreToPharmacyRNLine> lines = rnLineRepository.findAllByRnUidOrderByCreatedAtAsc(rn.getUid());
        return new RNDto(
                rn.getUid(),
                rn.getRnNo(),
                rn.getToUid(), to == null ? null : to.getToNo(),
                rn.getPharmacyUid(), pharmacy == null ? null : pharmacy.getName(),
                rn.getStoreUid(), store == null ? null : store.getName(),
                rn.getReceivingDate(),
                rn.getStatus(),
                rn.getCompletedAt(),
                rn.getCancelledAt(),
                rn.getNote(),
                rn.getCreatedAt(), rn.getUpdatedAt(),
                lines.stream().map(this::toRNLineDto).toList());
    }

    private RNLineDto toRNLineDto(StoreToPharmacyRNLine line) {
        Medicine m = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        MedicineUnit unit = lookupUnit(line.getUnitUid());
        return new RNLineDto(
                line.getUid(),
                line.getToLineUid(),
                line.getMedicineUid(),
                m == null ? null : m.getCode(),
                m == null ? null : m.getName(),
                m == null ? null : m.getStrength(),
                line.getUnitUid(),
                unit == null ? null : unit.getCode(),
                unit == null ? 1 : unit.getFactorToBase(),
                line.getIssuedQuantity(),
                line.getReceivedQuantity(),
                line.shortfall(),
                line.getCreatedAt());
    }

    private MedicineUnit lookupUnit(String unitUid) {
        if (unitUid == null || unitUid.isBlank()) return null;
        return medicineUnitRepository.findByUid(unitUid).orElse(null);
    }

    private RNSummary toRNSummary(StoreToPharmacyRN rn) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(rn.getPharmacyUid()).orElse(null);
        Store store = storeRepository.findByUid(rn.getStoreUid()).orElse(null);
        StoreToPharmacyTO to = toRepository.findByUid(rn.getToUid()).orElse(null);
        int lineCount = rnLineRepository.findAllByRnUidOrderByCreatedAtAsc(rn.getUid()).size();
        return new RNSummary(
                rn.getUid(),
                rn.getRnNo(),
                to == null ? null : to.getToNo(),
                pharmacy == null ? null : pharmacy.getName(),
                store == null ? null : store.getName(),
                rn.getReceivingDate(),
                rn.getStatus(),
                lineCount,
                rn.getCreatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
