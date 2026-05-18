package com.otapp.hmis.engine.masterdata.medicine.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnit;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper used by the stock + transfer services to convert quantities
 * typed by users in a particular {@link MedicineUnit} into the base-unit
 * integers that ledger and balance rows operate on.
 *
 * <p>Callers that pass a null {@code unitUid} get the medicine's base
 * unit by default — keeps single-unit callers (e.g. dispense, manual
 * receive) working unchanged.
 */
@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final MedicineUnitRepository unitRepository;

    /**
     * Resolve a unit by uid (or fall back to the base unit) and validate
     * that it belongs to {@code medicineUid}.
     */
    @Transactional(readOnly = true)
    public MedicineUnit resolveUnit(String medicineUid, String unitUid) {
        if (unitUid == null || unitUid.isBlank()) {
            return unitRepository.findByMedicineUidAndBaseTrue(medicineUid)
                    .orElseThrow(() -> new BusinessRuleException(
                            "No base unit configured for medicine: " + medicineUid));
        }
        MedicineUnit unit = unitRepository.findByUid(unitUid)
                .orElseThrow(() -> new NotFoundException("Medicine unit not found: " + unitUid));
        if (!unit.getMedicineUid().equals(medicineUid)) {
            throw new BusinessRuleException(
                    "Unit " + unit.getCode() + " does not belong to medicine " + medicineUid);
        }
        if (!unit.isActive()) {
            throw new BusinessRuleException("Medicine unit is not active: " + unit.getCode());
        }
        return unit;
    }

    /**
     * Convert {@code displayQuantity} (typed in {@code unit}) to its
     * integer base-unit equivalent, validating overflow.
     */
    public int toBaseQuantity(MedicineUnit unit, int displayQuantity) {
        if (displayQuantity <= 0) {
            throw new BusinessRuleException("Quantity must be positive");
        }
        long base = unit.toBase(displayQuantity);
        if (base > Integer.MAX_VALUE) {
            throw new BusinessRuleException("Quantity overflow when converting to base units");
        }
        return (int) base;
    }
}
