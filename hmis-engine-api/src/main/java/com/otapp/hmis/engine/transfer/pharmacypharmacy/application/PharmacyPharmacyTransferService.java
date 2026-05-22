package com.otapp.hmis.engine.transfer.pharmacypharmacy.application;

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
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.BatchPickResult;
import com.otapp.hmis.engine.pharmacy.stock.application.StockService;
import com.otapp.hmis.engine.transfer.common.domain.ReceiveNoteStatus;
import com.otapp.hmis.engine.transfer.common.domain.TransferDocStatus;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateROLineRequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateRORequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateRNLineRequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateRNRequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateTOLineRequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.CreateTORequest;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RNDto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RNLineDto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RNSummary;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.RODto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.ROLineDto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.ROSummary;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.TOBatchPickDto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.TODto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.TOLineDto;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.application.PharmacyPharmacyTransferDtos.TOSummary;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyRN;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyRNLine;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyRNLineRepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyRNRepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyRO;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyROLine;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyROLineRepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyRORepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyTO;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyTOBatchPick;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyTOBatchPickRepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyTOLine;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyTOLineRepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.domain.PharmacyToPharmacyTORepository;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.infrastructure.PharmacyToPharmacyRNNumberGenerator;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.infrastructure.PharmacyToPharmacyRONumberGenerator;
import com.otapp.hmis.engine.transfer.pharmacypharmacy.infrastructure.PharmacyToPharmacyTONumberGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the three-document pharmacy ↔ pharmacy transfer chain
 * (PROCESS.md §8.4). Lifecycle mirrors the P↔S chain but with both
 * endpoints being pharmacies: a requesting pharmacy raises an RO; once
 * APPROVED + SUBMITTED, the delivering pharmacy creates a TO against it;
 * issuing the TO decrements the delivering pharmacy's stock FEFO; the
 * requesting pharmacy then files an RN that increments its stock per
 * source batch and closes the chain.
 */
@Service
@RequiredArgsConstructor
public class PharmacyPharmacyTransferService {

    private final PharmacyToPharmacyRORepository roRepository;
    private final PharmacyToPharmacyROLineRepository roLineRepository;
    private final PharmacyToPharmacyTORepository toRepository;
    private final PharmacyToPharmacyTOLineRepository toLineRepository;
    private final PharmacyToPharmacyTOBatchPickRepository toPickRepository;
    private final PharmacyToPharmacyRNRepository rnRepository;
    private final PharmacyToPharmacyRNLineRepository rnLineRepository;

    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineUnitRepository medicineUnitRepository;
    private final UnitConversionService unitConversionService;
    private final StockService pharmacyStockService;

    private final PharmacyToPharmacyRONumberGenerator roNumberGenerator;
    private final PharmacyToPharmacyTONumberGenerator toNumberGenerator;
    private final PharmacyToPharmacyRNNumberGenerator rnNumberGenerator;

    // ========================================================================
    // RO operations
    // ========================================================================

    @Transactional
    public RODto createRO(CreateRORequest request) {
        Pharmacy requester = activePharmacy(request.requestingPharmacyUid());
        Pharmacy deliverer = activePharmacy(request.deliveringPharmacyUid());
        if (requester.getUid().equals(deliverer.getUid())) {
            throw new BusinessRuleException("Requesting and delivering pharmacy must differ");
        }

        PharmacyToPharmacyRO ro = roRepository.save(new PharmacyToPharmacyRO(
                roNumberGenerator.next(),
                requester.getUid(),
                deliverer.getUid(),
                null,
                request.validUntil(),
                emptyToNull(request.note())));

        for (CreateROLineRequest line : request.lines()) {
            Medicine medicine = activeMedicine(line.medicineUid());
            MedicineUnit unit = unitConversionService.resolveUnit(medicine.getUid(), emptyToNull(line.unitUid()));
            int baseQty = unitConversionService.toBaseQuantity(unit, line.quantity());
            roLineRepository.save(new PharmacyToPharmacyROLine(
                    ro.getUid(), medicine.getUid(), unit.getUid(), baseQty, emptyToNull(line.note())));
        }
        return toRODto(ro);
    }

    @Transactional
    public RODto verifyRO(String roUid) {
        PharmacyToPharmacyRO ro = loadRO(roUid);
        ro.verify();
        return toRODto(ro);
    }

    @Transactional
    public RODto approveRO(String roUid) {
        PharmacyToPharmacyRO ro = loadRO(roUid);
        ro.approve();
        return toRODto(ro);
    }

    @Transactional
    public RODto submitRO(String roUid) {
        PharmacyToPharmacyRO ro = loadRO(roUid);
        ro.submit();
        return toRODto(ro);
    }

    @Transactional
    public RODto rejectRO(String roUid, String reason) {
        PharmacyToPharmacyRO ro = loadRO(roUid);
        ro.reject(emptyToNull(reason));
        return toRODto(ro);
    }

    @Transactional
    public RODto returnRO(String roUid, String reason) {
        PharmacyToPharmacyRO ro = loadRO(roUid);
        ro.returnToRequester(emptyToNull(reason));
        return toRODto(ro);
    }

    @Transactional(readOnly = true)
    public RODto findRO(String roUid) {
        return toRODto(loadRO(roUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<ROSummary> searchROs(String query, TransferDocStatus status,
                                             String requestingPharmacyUid,
                                             String deliveringPharmacyUid, Pageable pageable) {
        return PageResponse.from(
                roRepository.search(emptyToNull(query), status,
                                emptyToNull(requestingPharmacyUid),
                                emptyToNull(deliveringPharmacyUid), pageable)
                        .map(this::toROSummary));
    }

    // ========================================================================
    // TO operations
    // ========================================================================

    @Transactional
    public TODto createTO(CreateTORequest request) {
        PharmacyToPharmacyRO ro = loadRO(request.roUid());
        if (ro.getStatus() != TransferDocStatus.SUBMITTED
                && ro.getStatus() != TransferDocStatus.IN_PROCESS) {
            throw new BusinessRuleException(
                    "TO can only be created against a SUBMITTED or IN_PROCESS RO (current: "
                            + ro.getStatus() + ")");
        }

        PharmacyToPharmacyTO to = toRepository.save(new PharmacyToPharmacyTO(
                toNumberGenerator.next(),
                ro.getUid(),
                ro.getRequestingPharmacyUid(),
                ro.getDeliveringPharmacyUid(),
                null,
                emptyToNull(request.note())));

        for (CreateTOLineRequest line : request.lines()) {
            PharmacyToPharmacyROLine roLine = roLineRepository.findByUid(line.roLineUid())
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
            toLineRepository.save(new PharmacyToPharmacyTOLine(
                    to.getUid(), roLine.getUid(), roLine.getMedicineUid(), unit.getUid(), baseQty));
        }

        ro.markInProcess();
        return toTODto(to);
    }

    @Transactional
    public TODto verifyTO(String toUid) {
        PharmacyToPharmacyTO to = loadTO(toUid);
        to.verify();
        return toTODto(to);
    }

    @Transactional
    public TODto approveTO(String toUid) {
        PharmacyToPharmacyTO to = loadTO(toUid);
        to.approve();
        return toTODto(to);
    }

    @Transactional
    public TODto rejectTO(String toUid, String reason) {
        PharmacyToPharmacyTO to = loadTO(toUid);
        to.reject(emptyToNull(reason));
        return toTODto(to);
    }

    /**
     * Decrements the delivering pharmacy's stock FEFO per TO line,
     * persists the resulting batch picks against the line, rolls
     * fulfilment forward on the parent RO, and advances both documents
     * to GOODS_ISSUED.
     */
    @Transactional
    public TODto issueTO(String toUid) {
        PharmacyToPharmacyTO to = loadTO(toUid);
        PharmacyToPharmacyRO ro = loadRO(to.getRoUid());

        List<PharmacyToPharmacyTOLine> lines = toLineRepository.findAllByToUidOrderByCreatedAtAsc(to.getUid());
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Cannot issue a TO with no lines");
        }
        for (PharmacyToPharmacyTOLine line : lines) {
            int outstanding = line.getRequestedQuantity() - line.getIssuedQuantity();
            if (outstanding <= 0) continue;

            List<BatchPickResult> picks = pharmacyStockService.issueToPharmacy(
                    to.getDeliveringPharmacyUid(), line.getMedicineUid(), outstanding,
                    to.getUid(), "Issue for TO " + to.getToNo());

            for (BatchPickResult pick : picks) {
                toPickRepository.save(new PharmacyToPharmacyTOBatchPick(
                        line.getUid(),
                        pick.batchUid(),
                        pick.batchNo(),
                        pick.expiresAt(),
                        pick.quantity()));
            }
            line.recordIssue(outstanding);

            PharmacyToPharmacyROLine roLine = roLineRepository.findByUid(line.getRoLineUid())
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
                                             String requestingPharmacyUid,
                                             String deliveringPharmacyUid,
                                             String roUid, Pageable pageable) {
        return PageResponse.from(
                toRepository.search(emptyToNull(query), status,
                                emptyToNull(requestingPharmacyUid),
                                emptyToNull(deliveringPharmacyUid),
                                emptyToNull(roUid), pageable)
                        .map(this::toTOSummary));
    }

    // ========================================================================
    // RN operations
    // ========================================================================

    @Transactional
    public RNDto createRN(CreateRNRequest request) {
        PharmacyToPharmacyTO to = loadTO(request.toUid());
        PharmacyToPharmacyRO ro = loadRO(to.getRoUid());

        if (to.getStatus() != TransferDocStatus.GOODS_ISSUED) {
            throw new BusinessRuleException(
                    "RN can only be filed against a GOODS_ISSUED TO (current: " + to.getStatus() + ")");
        }
        if (rnRepository.existsByToUidAndStatus(to.getUid(), ReceiveNoteStatus.COMPLETED)) {
            throw new BusinessRuleException("TO has already been received");
        }

        PharmacyToPharmacyRN rn = rnRepository.save(new PharmacyToPharmacyRN(
                rnNumberGenerator.next(),
                to.getUid(),
                to.getRequestingPharmacyUid(),
                to.getDeliveringPharmacyUid(),
                request.receivingDate(),
                emptyToNull(request.note())));

        for (CreateRNLineRequest lineReq : request.lines()) {
            PharmacyToPharmacyTOLine toLine = toLineRepository.findByUid(lineReq.toLineUid())
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

            PharmacyToPharmacyRNLine rnLine = rnLineRepository.save(new PharmacyToPharmacyRNLine(
                    rn.getUid(),
                    toLine.getUid(),
                    toLine.getMedicineUid(),
                    unit.getUid(),
                    toLine.getIssuedQuantity(),
                    receivedBase));

            if (receivedBase > 0) {
                allocateAcrossPicks(to, toLine, rnLine, receivedBase);
                toLine.recordReceipt(receivedBase);
            }
        }

        rn.markCompleted();
        to.markCompleted();
        ro.markCompleted();
        return toRNDto(rn);
    }

    private void allocateAcrossPicks(PharmacyToPharmacyTO to, PharmacyToPharmacyTOLine toLine,
                                     PharmacyToPharmacyRNLine rnLine, int received) {
        List<PharmacyToPharmacyTOBatchPick> picks = toPickRepository
                .findAllByToLineUidOrderByCreatedAtAsc(toLine.getUid());
        int remaining = received;
        for (PharmacyToPharmacyTOBatchPick pick : picks) {
            if (remaining <= 0) break;
            int credit = Math.min(remaining, pick.getQuantity());
            pharmacyStockService.receiveFromPharmacy(
                    to.getRequestingPharmacyUid(),
                    toLine.getMedicineUid(),
                    pick.getBatchNo(),
                    pick.getExpiresAt(),
                    credit,
                    to.getUid(),
                    "RN " + rnLine.getRnUid() + " pick " + pick.getUid());
            pick.setRnLineUid(rnLine.getUid());
            remaining -= credit;
        }
        if (remaining > 0) {
            throw new BusinessRuleException(
                    "Received qty exceeds sum of TO picks — TO data is inconsistent");
        }
    }

    @Transactional(readOnly = true)
    public RNDto findRN(String rnUid) {
        return toRNDto(loadRN(rnUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<RNSummary> searchRNs(String query, ReceiveNoteStatus status,
                                             String requestingPharmacyUid,
                                             String deliveringPharmacyUid,
                                             String toUid, Pageable pageable) {
        return PageResponse.from(
                rnRepository.search(emptyToNull(query), status,
                                emptyToNull(requestingPharmacyUid),
                                emptyToNull(deliveringPharmacyUid),
                                emptyToNull(toUid), pageable)
                        .map(this::toRNSummary));
    }

    // ========================================================================
    // Loaders + mapping
    // ========================================================================

    private PharmacyToPharmacyRO loadRO(String uid) {
        return roRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("RO not found: " + uid));
    }

    private PharmacyToPharmacyTO loadTO(String uid) {
        return toRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("TO not found: " + uid));
    }

    private PharmacyToPharmacyRN loadRN(String uid) {
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

    private Medicine activeMedicine(String uid) {
        Medicine m = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!m.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + m.getName());
        }
        return m;
    }

    private RODto toRODto(PharmacyToPharmacyRO ro) {
        Pharmacy requester = pharmacyRepository.findByUid(ro.getRequestingPharmacyUid()).orElse(null);
        Pharmacy deliverer = pharmacyRepository.findByUid(ro.getDeliveringPharmacyUid()).orElse(null);
        List<PharmacyToPharmacyROLine> lines = roLineRepository.findAllByRoUidOrderByCreatedAtAsc(ro.getUid());
        return new RODto(
                ro.getUid(),
                ro.getRoNo(),
                ro.getRequestingPharmacyUid(), requester == null ? null : requester.getName(),
                ro.getDeliveringPharmacyUid(), deliverer == null ? null : deliverer.getName(),
                ro.getOrderDate(), ro.getValidUntil(),
                ro.getStatus(),
                ro.getVerifiedAt(), ro.getApprovedAt(), ro.getSubmittedAt(),
                ro.getInProcessAt(), ro.getIssuedAt(), ro.getCompletedAt(),
                ro.getRejectedAt(), ro.getReturnedAt(),
                ro.getRejectReason(), ro.getNote(),
                ro.getCreatedAt(), ro.getUpdatedAt(),
                lines.stream().map(this::toROLineDto).toList());
    }

    private ROLineDto toROLineDto(PharmacyToPharmacyROLine line) {
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

    private ROSummary toROSummary(PharmacyToPharmacyRO ro) {
        Pharmacy requester = pharmacyRepository.findByUid(ro.getRequestingPharmacyUid()).orElse(null);
        Pharmacy deliverer = pharmacyRepository.findByUid(ro.getDeliveringPharmacyUid()).orElse(null);
        int lineCount = roLineRepository.findAllByRoUidOrderByCreatedAtAsc(ro.getUid()).size();
        return new ROSummary(
                ro.getUid(),
                ro.getRoNo(),
                requester == null ? null : requester.getName(),
                deliverer == null ? null : deliverer.getName(),
                ro.getOrderDate(),
                ro.getStatus(),
                lineCount,
                ro.getCreatedAt());
    }

    private TODto toTODto(PharmacyToPharmacyTO to) {
        Pharmacy requester = pharmacyRepository.findByUid(to.getRequestingPharmacyUid()).orElse(null);
        Pharmacy deliverer = pharmacyRepository.findByUid(to.getDeliveringPharmacyUid()).orElse(null);
        PharmacyToPharmacyRO ro = roRepository.findByUid(to.getRoUid()).orElse(null);
        List<PharmacyToPharmacyTOLine> lines = toLineRepository.findAllByToUidOrderByCreatedAtAsc(to.getUid());
        return new TODto(
                to.getUid(),
                to.getToNo(),
                to.getRoUid(), ro == null ? null : ro.getRoNo(),
                to.getRequestingPharmacyUid(), requester == null ? null : requester.getName(),
                to.getDeliveringPharmacyUid(), deliverer == null ? null : deliverer.getName(),
                to.getOrderDate(),
                to.getStatus(),
                to.getVerifiedAt(), to.getApprovedAt(), to.getIssuedAt(),
                to.getCompletedAt(), to.getRejectedAt(),
                to.getRejectedReason(), to.getNote(),
                to.getCreatedAt(), to.getUpdatedAt(),
                lines.stream().map(this::toTOLineDto).toList());
    }

    private TOLineDto toTOLineDto(PharmacyToPharmacyTOLine line) {
        Medicine m = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        MedicineUnit unit = lookupUnit(line.getUnitUid());
        List<PharmacyToPharmacyTOBatchPick> picks = toPickRepository
                .findAllByToLineUidOrderByCreatedAtAsc(line.getUid());
        List<TOBatchPickDto> pickDtos = new ArrayList<>(picks.size());
        for (PharmacyToPharmacyTOBatchPick p : picks) {
            pickDtos.add(new TOBatchPickDto(
                    p.getSourceBatchUid(), p.getBatchNo(), p.getExpiresAt(),
                    p.getQuantity(), p.getRnLineUid()));
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

    private TOSummary toTOSummary(PharmacyToPharmacyTO to) {
        Pharmacy requester = pharmacyRepository.findByUid(to.getRequestingPharmacyUid()).orElse(null);
        Pharmacy deliverer = pharmacyRepository.findByUid(to.getDeliveringPharmacyUid()).orElse(null);
        PharmacyToPharmacyRO ro = roRepository.findByUid(to.getRoUid()).orElse(null);
        int lineCount = toLineRepository.findAllByToUidOrderByCreatedAtAsc(to.getUid()).size();
        return new TOSummary(
                to.getUid(),
                to.getToNo(),
                ro == null ? null : ro.getRoNo(),
                requester == null ? null : requester.getName(),
                deliverer == null ? null : deliverer.getName(),
                to.getOrderDate(),
                to.getStatus(),
                lineCount,
                to.getCreatedAt());
    }

    private RNDto toRNDto(PharmacyToPharmacyRN rn) {
        Pharmacy requester = pharmacyRepository.findByUid(rn.getRequestingPharmacyUid()).orElse(null);
        Pharmacy deliverer = pharmacyRepository.findByUid(rn.getDeliveringPharmacyUid()).orElse(null);
        PharmacyToPharmacyTO to = toRepository.findByUid(rn.getToUid()).orElse(null);
        List<PharmacyToPharmacyRNLine> lines = rnLineRepository.findAllByRnUidOrderByCreatedAtAsc(rn.getUid());
        return new RNDto(
                rn.getUid(),
                rn.getRnNo(),
                rn.getToUid(), to == null ? null : to.getToNo(),
                rn.getRequestingPharmacyUid(), requester == null ? null : requester.getName(),
                rn.getDeliveringPharmacyUid(), deliverer == null ? null : deliverer.getName(),
                rn.getReceivingDate(),
                rn.getStatus(),
                rn.getCompletedAt(),
                rn.getCancelledAt(),
                rn.getNote(),
                rn.getCreatedAt(), rn.getUpdatedAt(),
                lines.stream().map(this::toRNLineDto).toList());
    }

    private RNLineDto toRNLineDto(PharmacyToPharmacyRNLine line) {
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

    private RNSummary toRNSummary(PharmacyToPharmacyRN rn) {
        Pharmacy requester = pharmacyRepository.findByUid(rn.getRequestingPharmacyUid()).orElse(null);
        Pharmacy deliverer = pharmacyRepository.findByUid(rn.getDeliveringPharmacyUid()).orElse(null);
        PharmacyToPharmacyTO to = toRepository.findByUid(rn.getToUid()).orElse(null);
        int lineCount = rnLineRepository.findAllByRnUidOrderByCreatedAtAsc(rn.getUid()).size();
        return new RNSummary(
                rn.getUid(),
                rn.getRnNo(),
                to == null ? null : to.getToNo(),
                requester == null ? null : requester.getName(),
                deliverer == null ? null : deliverer.getName(),
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
