package com.otapp.hmis.engine.masterdata.dosage.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageDtos.CreateDosageRequest;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageDtos.DosageDto;
import com.otapp.hmis.engine.masterdata.dosage.application.DosageDtos.UpdateDosageRequest;
import com.otapp.hmis.engine.masterdata.dosage.domain.Dosage;
import com.otapp.hmis.engine.masterdata.dosage.domain.DosageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DosageService {

    private final DosageRepository repo;

    @Transactional
    public DosageDto create(CreateDosageRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Dosage code already exists: " + code);
        }
        Dosage d = new Dosage(code, request.name().trim(), request.description());
        repo.save(d);
        return toDto(d);
    }

    @Transactional
    public DosageDto update(String uid, UpdateDosageRequest request) {
        Dosage d = loadOrThrow(uid);
        d.setName(request.name().trim());
        d.setDescription(request.description());
        return toDto(d);
    }

    @Transactional
    public DosageDto setActive(String uid, boolean active) {
        Dosage d = loadOrThrow(uid);
        if (active) d.activate(); else d.deactivate();
        return toDto(d);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public DosageDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<DosageDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable)
                        .map(DosageService::toDto));
    }

    private Dosage loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Dosage not found: " + uid));
    }

    private static DosageDto toDto(Dosage d) {
        return new DosageDto(d.getUid(), d.getCode(), d.getName(), d.getDescription(),
                d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
