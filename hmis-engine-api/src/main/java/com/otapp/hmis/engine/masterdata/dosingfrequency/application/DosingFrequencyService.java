package com.otapp.hmis.engine.masterdata.dosingfrequency.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyDtos.CreateDosingFrequencyRequest;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyDtos.DosingFrequencyDto;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyDtos.UpdateDosingFrequencyRequest;
import com.otapp.hmis.engine.masterdata.dosingfrequency.domain.DosingFrequency;
import com.otapp.hmis.engine.masterdata.dosingfrequency.domain.DosingFrequencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DosingFrequencyService {

    private final DosingFrequencyRepository repo;

    @Transactional
    public DosingFrequencyDto create(CreateDosingFrequencyRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Dosing-frequency code already exists: " + code);
        }
        DosingFrequency f = new DosingFrequency(code, request.name().trim(),
                request.timesPerDay(), request.description());
        repo.save(f);
        return toDto(f);
    }

    @Transactional
    public DosingFrequencyDto update(String uid, UpdateDosingFrequencyRequest request) {
        DosingFrequency f = loadOrThrow(uid);
        f.setName(request.name().trim());
        f.setTimesPerDay(request.timesPerDay());
        f.setDescription(request.description());
        return toDto(f);
    }

    @Transactional
    public DosingFrequencyDto setActive(String uid, boolean active) {
        DosingFrequency f = loadOrThrow(uid);
        if (active) f.activate(); else f.deactivate();
        return toDto(f);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public DosingFrequencyDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<DosingFrequencyDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable)
                        .map(DosingFrequencyService::toDto));
    }

    private DosingFrequency loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Dosing frequency not found: " + uid));
    }

    private static DosingFrequencyDto toDto(DosingFrequency f) {
        return new DosingFrequencyDto(f.getUid(), f.getCode(), f.getName(),
                f.getTimesPerDay(), f.getDescription(),
                f.isActive(), f.getCreatedAt(), f.getUpdatedAt());
    }
}
