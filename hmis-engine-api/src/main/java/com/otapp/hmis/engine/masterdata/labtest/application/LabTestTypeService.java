package com.otapp.hmis.engine.masterdata.labtest.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeDtos.CreateLabTestTypeRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeDtos.LabTestTypeDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestTypeDtos.UpdateLabTestTypeRequest;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestType;
import com.otapp.hmis.engine.masterdata.labtest.domain.LabTestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabTestTypeService {

    private final LabTestTypeRepository repo;

    @Transactional
    public LabTestTypeDto create(CreateLabTestTypeRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Lab test code already exists: " + code);
        }
        LabTestType l = new LabTestType(code, request.name().trim(), request.specimen(),
                request.unit(), request.description());
        repo.save(l);
        return toDto(l);
    }

    @Transactional
    public LabTestTypeDto update(String uid, UpdateLabTestTypeRequest request) {
        LabTestType l = loadOrThrow(uid);
        l.setName(request.name().trim());
        l.setSpecimen(request.specimen());
        l.setUnit(request.unit());
        l.setDescription(request.description());
        return toDto(l);
    }

    @Transactional
    public LabTestTypeDto setActive(String uid, boolean active) {
        LabTestType l = loadOrThrow(uid);
        if (active) l.activate(); else l.deactivate();
        return toDto(l);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public LabTestTypeDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<LabTestTypeDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable).map(LabTestTypeService::toDto));
    }

    private LabTestType loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Lab test not found: " + uid));
    }

    private static LabTestTypeDto toDto(LabTestType l) {
        return new LabTestTypeDto(l.getUid(), l.getCode(), l.getName(), l.getSpecimen(),
                l.getUnit(), l.getDescription(), l.isActive(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
