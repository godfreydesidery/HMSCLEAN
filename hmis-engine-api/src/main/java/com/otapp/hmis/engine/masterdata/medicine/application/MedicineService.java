package com.otapp.hmis.engine.masterdata.medicine.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.CreateMedicineRequest;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.MedicineDto;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.UpdateMedicineRequest;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineForm;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository repo;

    @Transactional
    public MedicineDto create(CreateMedicineRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Medicine code already exists: " + code);
        }
        Medicine m = new Medicine(code, request.name().trim(), request.genericName(),
                request.strength(), request.form(), request.description());
        repo.save(m);
        return toDto(m);
    }

    @Transactional
    public MedicineDto update(String uid, UpdateMedicineRequest request) {
        Medicine m = loadOrThrow(uid);
        m.setName(request.name().trim());
        m.setGenericName(request.genericName());
        m.setStrength(request.strength());
        m.setForm(request.form());
        m.setDescription(request.description());
        return toDto(m);
    }

    @Transactional
    public MedicineDto setActive(String uid, boolean active) {
        Medicine m = loadOrThrow(uid);
        if (active) m.activate(); else m.deactivate();
        return toDto(m);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public MedicineDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<MedicineDto> search(String query, Boolean active, MedicineForm form, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, form, pageable).map(MedicineService::toDto));
    }

    private Medicine loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
    }

    private static MedicineDto toDto(Medicine m) {
        return new MedicineDto(m.getUid(), m.getCode(), m.getName(), m.getGenericName(),
                m.getStrength(), m.getForm(), m.getDescription(),
                m.isActive(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
