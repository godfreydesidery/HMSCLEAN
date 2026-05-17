package com.otapp.hmis.engine.pharmacy.stock.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.encounter.prescription.domain.Prescription;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionRepository;
import com.otapp.hmis.engine.encounter.prescription.domain.PrescriptionStatus;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.AdjustStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.ReceiveStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockBalanceDto;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockMovementDto;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleLineStatus;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrder;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderLine;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderLineRepository;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBalance;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockBalanceRepository;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovement;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementKind;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementRepository;
import java.util.EnumSet;
import java.util.List;
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
    private final StockMovementRepository movementRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PharmacySaleOrderRepository saleRepository;
    private final PharmacySaleOrderLineRepository saleLineRepository;

    @Transactional
    public StockBalanceDto receive(String pharmacyUid, ReceiveStockRequest request) {
        return receiveInternal(pharmacyUid, request.medicineUid(), request.quantity(),
                null, emptyToNull(request.note()));
    }

    /**
     * Internal cross-module entry point used by procurement (goods receipt)
     * to apply a RECEIPT movement against a stock balance and link it back
     * to the source document via {@code referenceUid}.
     */
    @Transactional
    public StockBalanceDto receiveForReference(String pharmacyUid, String medicineUid,
                                               int quantity, String referenceUid, String note) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Receipt quantity must be positive");
        }
        return receiveInternal(pharmacyUid, medicineUid, quantity, emptyToNull(referenceUid), emptyToNull(note));
    }

    private StockBalanceDto receiveInternal(String pharmacyUid, String medicineUid,
                                            int quantity, String referenceUid, String note) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        Medicine medicine = activeMedicine(medicineUid);
        StockBalance balance = lockOrCreate(pharmacy.getUid(), medicine.getUid());
        balance.applyDelta(quantity);
        recordMovement(balance, StockMovementKind.RECEIPT, quantity, referenceUid, note);
        return toBalanceDto(balance, pharmacy, medicine);
    }

    @Transactional
    public StockBalanceDto adjust(String pharmacyUid, AdjustStockRequest request) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        Medicine medicine = activeMedicine(request.medicineUid());
        if (request.delta() == 0) {
            throw new BusinessRuleException("Adjustment delta must be non-zero");
        }
        StockBalance balance = lockOrCreate(pharmacy.getUid(), medicine.getUid());

        balance.applyDelta(request.delta());
        recordMovement(balance, StockMovementKind.ADJUSTMENT, request.delta(),
                null, emptyToNull(request.note()));
        return toBalanceDto(balance, pharmacy, medicine);
    }

    /**
     * Decrements stock to fulfil a prescription that has already moved
     * through PENDING → ACCEPTED → VERIFIED → APPROVED, marks the
     * prescription as SOLD, and records a DISPENSE movement linked back
     * to the rx. Refuses anything pre-APPROVED — clinical / payment gates
     * are the pharmacy's job before this point.
     */
    @Transactional
    public StockMovementDto dispense(String pharmacyUid, String prescriptionUid) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
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

        StockBalance balance = balanceRepository.lockByPharmacyUidAndMedicineUid(pharmacy.getUid(), medicine.getUid())
                .orElseThrow(() -> new BusinessRuleException(
                        "No stock for " + medicine.getName() + " at " + pharmacy.getName()));
        balance.applyDelta(-rx.getQuantity());

        StockMovement movement = recordMovement(balance, StockMovementKind.DISPENSE,
                -rx.getQuantity(), rx.getUid(),
                "Dispense for " + rx.getPrescriptionNo());

        rx.markSold();

        return toMovementDto(movement, pharmacy, medicine);
    }

    /**
     * Final dispense of a retail sale line. Symmetric to the prescription
     * dispense but driven by {@link PharmacySaleOrderLine}: lock balance,
     * validate APPROVED + non-zero qty, decrement, record a DISPENSE
     * movement referencing the line uid, transition the line to SOLD, and
     * roll the sale-order header forward if every line is now terminal.
     */
    @Transactional
    public StockMovementDto dispenseSaleLine(String pharmacyUid, String saleLineUid) {
        Pharmacy pharmacy = activePharmacy(pharmacyUid);
        PharmacySaleOrderLine line = saleLineRepository.findByUid(saleLineUid)
                .orElseThrow(() -> new NotFoundException("Sale line not found: " + saleLineUid));
        if (line.getStatus() != PharmacySaleLineStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED sale lines can be dispensed (current: " + line.getStatus()
                            + "). Move through accept → verify → approve first.");
        }
        if (line.getQuantity() <= 0) {
            throw new BusinessRuleException("Sale line has no quantity set");
        }
        PharmacySaleOrder sale = saleRepository.findByUid(line.getSaleUid())
                .orElseThrow(() -> new NotFoundException("Sale not found: " + line.getSaleUid()));
        if (!sale.getPharmacyUid().equals(pharmacy.getUid())) {
            throw new BusinessRuleException("Sale was opened at a different pharmacy");
        }
        Medicine medicine = medicineRepository.findByUid(line.getMedicineUid())
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + line.getMedicineUid()));

        StockBalance balance = balanceRepository.lockByPharmacyUidAndMedicineUid(pharmacy.getUid(), medicine.getUid())
                .orElseThrow(() -> new BusinessRuleException(
                        "No stock for " + medicine.getName() + " at " + pharmacy.getName()));
        balance.applyDelta(-line.getQuantity());

        StockMovement movement = recordMovement(balance, StockMovementKind.DISPENSE,
                -line.getQuantity(), line.getUid(),
                "Sale dispense for " + sale.getSaleNo());

        line.markSold();

        long openLines = saleLineRepository.countBySaleUidAndStatusNotIn(sale.getUid(), TERMINAL_SALE_LINE_STATUSES);
        sale.onLineTransition((int) openLines);

        return toMovementDto(movement, pharmacy, medicine);
    }

    @Transactional(readOnly = true)
    public List<StockBalanceDto> listBalances(String pharmacyUid) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(pharmacyUid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + pharmacyUid));
        return balanceRepository.findAllByPharmacyUid(pharmacy.getUid()).stream()
                .map(b -> toBalanceDto(b, pharmacy, medicineRepository.findByUid(b.getMedicineUid()).orElse(null)))
                .toList();
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

    private StockBalance lockOrCreate(String pharmacyUid, String medicineUid) {
        return balanceRepository.lockByPharmacyUidAndMedicineUid(pharmacyUid, medicineUid)
                .orElseGet(() -> balanceRepository.save(new StockBalance(pharmacyUid, medicineUid)));
    }

    private StockMovement recordMovement(StockBalance balance, StockMovementKind kind,
                                         int delta, String referenceUid, String note) {
        return movementRepository.save(new StockMovement(
                balance.getPharmacyUid(), balance.getMedicineUid(), kind,
                delta, balance.getQuantity(), referenceUid, note, currentUsername()));
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static StockBalanceDto toBalanceDto(StockBalance b, Pharmacy pharmacy, Medicine medicine) {
        return new StockBalanceDto(
                b.getUid(),
                b.getPharmacyUid(),
                pharmacy == null ? null : pharmacy.getName(),
                b.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getStrength(),
                b.getQuantity(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }

    private static StockMovementDto toMovementDto(StockMovement m, Pharmacy pharmacy, Medicine medicine) {
        return new StockMovementDto(
                m.getUid(),
                m.getPharmacyUid(),
                pharmacy == null ? null : pharmacy.getName(),
                m.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
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
