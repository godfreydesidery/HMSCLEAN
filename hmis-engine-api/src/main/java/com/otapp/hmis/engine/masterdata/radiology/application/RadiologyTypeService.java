package com.otapp.hmis.engine.masterdata.radiology.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeDtos.CreateRadiologyTypeRequest;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeDtos.RadiologyTypeDto;
import com.otapp.hmis.engine.masterdata.radiology.application.RadiologyTypeDtos.UpdateRadiologyTypeRequest;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyModality;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyType;
import com.otapp.hmis.engine.masterdata.radiology.domain.RadiologyTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RadiologyTypeService {

    private final RadiologyTypeRepository repo;

    @Transactional
    public RadiologyTypeDto create(CreateRadiologyTypeRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Radiology code already exists: " + code);
        }
        RadiologyType r = new RadiologyType(code, request.name().trim(), request.modality(), request.description());
        repo.save(r);
        return toDto(r);
    }

    @Transactional
    public RadiologyTypeDto update(String uid, UpdateRadiologyTypeRequest request) {
        RadiologyType r = loadOrThrow(uid);
        r.setName(request.name().trim());
        r.setModality(request.modality());
        r.setDescription(request.description());
        return toDto(r);
    }

    @Transactional
    public RadiologyTypeDto setActive(String uid, boolean active) {
        RadiologyType r = loadOrThrow(uid);
        if (active) r.activate(); else r.deactivate();
        return toDto(r);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public RadiologyTypeDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<RadiologyTypeDto> search(String query, Boolean active, RadiologyModality modality, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, modality, pageable).map(RadiologyTypeService::toDto));
    }

    private RadiologyType loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Radiology not found: " + uid));
    }

    private static RadiologyTypeDto toDto(RadiologyType r) {
        return new RadiologyTypeDto(r.getUid(), r.getCode(), r.getName(), r.getModality(),
                r.getDescription(), r.isActive(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
