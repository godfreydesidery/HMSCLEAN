package com.otapp.hmis.engine.masterdata.medicine.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.CreateMedicineUnitRequest;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.MedicineUnitDto;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.UpdateMedicineUnitRequest;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnit;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnitRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicineUnitService {

    private final MedicineUnitRepository unitRepository;
    private final MedicineRepository medicineRepository;

    @Transactional(readOnly = true)
    public List<MedicineUnitDto> list(String medicineUid) {
        loadMedicineOrThrow(medicineUid);
        return unitRepository.findAllByMedicineUidOrderByBaseDescCodeAsc(medicineUid).stream()
                .map(MedicineUnitService::toDto)
                .toList();
    }

    @Transactional
    public MedicineUnitDto create(String medicineUid, CreateMedicineUnitRequest request) {
        loadMedicineOrThrow(medicineUid);
        String code = request.code().trim().toUpperCase();
        if (unitRepository.existsByMedicineUidAndCode(medicineUid, code)) {
            throw new ConflictException("Unit code already exists for this medicine: " + code);
        }
        // Created units are never the base — base is auto-created with the medicine.
        MedicineUnit unit = unitRepository.save(new MedicineUnit(
                medicineUid, code, request.name().trim(), request.factorToBase(), false));
        return toDto(unit);
    }

    @Transactional
    public MedicineUnitDto update(String medicineUid, String unitUid, UpdateMedicineUnitRequest request) {
        MedicineUnit unit = loadOwned(medicineUid, unitUid);
        if (unit.isBase() && request.factorToBase() != 1) {
            throw new BusinessRuleException("Base unit factor cannot be changed");
        }
        unit.setName(request.name().trim());
        unit.setFactorToBase(request.factorToBase());
        return toDto(unit);
    }

    @Transactional
    public MedicineUnitDto setActive(String medicineUid, String unitUid, boolean active) {
        MedicineUnit unit = loadOwned(medicineUid, unitUid);
        if (unit.isBase() && !active) {
            throw new BusinessRuleException("Base unit cannot be deactivated");
        }
        unit.setActive(active);
        return toDto(unit);
    }

    @Transactional
    public void delete(String medicineUid, String unitUid) {
        MedicineUnit unit = loadOwned(medicineUid, unitUid);
        if (unit.isBase()) {
            throw new BusinessRuleException("Base unit cannot be deleted");
        }
        unitRepository.delete(unit);
    }

    private Medicine loadMedicineOrThrow(String medicineUid) {
        return medicineRepository.findByUid(medicineUid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + medicineUid));
    }

    private MedicineUnit loadOwned(String medicineUid, String unitUid) {
        MedicineUnit unit = unitRepository.findByUid(unitUid)
                .orElseThrow(() -> new NotFoundException("Medicine unit not found: " + unitUid));
        if (!unit.getMedicineUid().equals(medicineUid)) {
            throw new BusinessRuleException("Unit does not belong to this medicine");
        }
        return unit;
    }

    private static MedicineUnitDto toDto(MedicineUnit u) {
        return new MedicineUnitDto(
                u.getUid(),
                u.getMedicineUid(),
                u.getCode(),
                u.getName(),
                u.getFactorToBase(),
                u.isBase(),
                u.isActive(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
