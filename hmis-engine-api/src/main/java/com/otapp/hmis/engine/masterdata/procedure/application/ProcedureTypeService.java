package com.otapp.hmis.engine.masterdata.procedure.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeDtos.CreateProcedureTypeRequest;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeDtos.ProcedureTypeDto;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeDtos.UpdateProcedureTypeRequest;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureType;
import com.otapp.hmis.engine.masterdata.procedure.domain.ProcedureTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcedureTypeService {

    private final ProcedureTypeRepository repo;

    @Transactional
    public ProcedureTypeDto create(CreateProcedureTypeRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Procedure code already exists: " + code);
        }
        ProcedureType p = new ProcedureType(code, request.name().trim(), request.description());
        repo.save(p);
        return toDto(p);
    }

    @Transactional
    public ProcedureTypeDto update(String uid, UpdateProcedureTypeRequest request) {
        ProcedureType p = loadOrThrow(uid);
        p.setName(request.name().trim());
        p.setDescription(request.description());
        return toDto(p);
    }

    @Transactional
    public ProcedureTypeDto setActive(String uid, boolean active) {
        ProcedureType p = loadOrThrow(uid);
        if (active) p.activate(); else p.deactivate();
        return toDto(p);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public ProcedureTypeDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<ProcedureTypeDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable).map(ProcedureTypeService::toDto));
    }

    private ProcedureType loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Procedure not found: " + uid));
    }

    private static ProcedureTypeDto toDto(ProcedureType p) {
        return new ProcedureTypeDto(p.getUid(), p.getCode(), p.getName(), p.getDescription(),
                p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
