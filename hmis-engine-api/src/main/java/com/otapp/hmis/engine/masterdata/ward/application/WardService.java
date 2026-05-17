package com.otapp.hmis.engine.masterdata.ward.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.ward.application.dto.CreateWardRequest;
import com.otapp.hmis.engine.masterdata.ward.application.dto.UpdateWardRequest;
import com.otapp.hmis.engine.masterdata.ward.application.dto.WardDto;
import com.otapp.hmis.engine.masterdata.ward.domain.Ward;
import com.otapp.hmis.engine.masterdata.ward.domain.WardCategory;
import com.otapp.hmis.engine.masterdata.ward.domain.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WardService {

    private final WardRepository wardRepository;

    @Transactional
    public WardDto create(CreateWardRequest request) {
        String code = request.code().trim().toUpperCase();
        if (wardRepository.existsByCode(code)) {
            throw new ConflictException("Ward code already exists: " + code);
        }
        Ward ward = new Ward(code, request.name().trim(), request.category(),
                request.capacity(), request.location(), request.description());
        wardRepository.save(ward);
        return toDto(ward);
    }

    @Transactional
    public WardDto update(String uid, UpdateWardRequest request) {
        Ward w = loadOrThrow(uid);
        w.setName(request.name().trim());
        w.setCategory(request.category());
        w.setCapacity(request.capacity());
        w.setLocation(request.location());
        w.setDescription(request.description());
        return toDto(w);
    }

    @Transactional
    public WardDto setActive(String uid, boolean active) {
        Ward w = loadOrThrow(uid);
        if (active) w.activate(); else w.deactivate();
        return toDto(w);
    }

    @Transactional
    public void delete(String uid) {
        wardRepository.delete(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public WardDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<WardDto> search(String query, Boolean active, WardCategory category, Pageable pageable) {
        String trimmed = query == null ? null : query.trim();
        return PageResponse.from(wardRepository.search(trimmed, active, category, pageable).map(WardService::toDto));
    }

    private Ward loadOrThrow(String uid) {
        return wardRepository.findByUid(uid).orElseThrow(() -> new NotFoundException("Ward not found: " + uid));
    }

    private static WardDto toDto(Ward w) {
        return new WardDto(w.getUid(), w.getCode(), w.getName(), w.getCategory(),
                w.getCapacity(), w.getLocation(), w.getDescription(), w.isActive(),
                w.getCreatedAt(), w.getUpdatedAt());
    }
}
