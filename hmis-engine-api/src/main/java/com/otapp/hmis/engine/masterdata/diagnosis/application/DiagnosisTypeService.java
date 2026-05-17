package com.otapp.hmis.engine.masterdata.diagnosis.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeDtos.CreateDiagnosisTypeRequest;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeDtos.DiagnosisTypeDto;
import com.otapp.hmis.engine.masterdata.diagnosis.application.DiagnosisTypeDtos.UpdateDiagnosisTypeRequest;
import com.otapp.hmis.engine.masterdata.diagnosis.domain.DiagnosisType;
import com.otapp.hmis.engine.masterdata.diagnosis.domain.DiagnosisTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiagnosisTypeService {

    private final DiagnosisTypeRepository repo;

    @Transactional
    public DiagnosisTypeDto create(CreateDiagnosisTypeRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Diagnosis code already exists: " + code);
        }
        DiagnosisType d = new DiagnosisType(code, request.name().trim(), request.description());
        repo.save(d);
        return toDto(d);
    }

    @Transactional
    public DiagnosisTypeDto update(String uid, UpdateDiagnosisTypeRequest request) {
        DiagnosisType d = loadOrThrow(uid);
        d.setName(request.name().trim());
        d.setDescription(request.description());
        return toDto(d);
    }

    @Transactional
    public DiagnosisTypeDto setActive(String uid, boolean active) {
        DiagnosisType d = loadOrThrow(uid);
        if (active) d.activate(); else d.deactivate();
        return toDto(d);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public DiagnosisTypeDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<DiagnosisTypeDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable).map(DiagnosisTypeService::toDto));
    }

    private DiagnosisType loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Diagnosis not found: " + uid));
    }

    private static DiagnosisTypeDto toDto(DiagnosisType d) {
        return new DiagnosisTypeDto(d.getUid(), d.getCode(), d.getName(), d.getDescription(),
                d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
