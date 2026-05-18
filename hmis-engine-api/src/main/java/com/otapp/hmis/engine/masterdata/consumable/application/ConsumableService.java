package com.otapp.hmis.engine.masterdata.consumable.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableDtos.ConsumableDto;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableDtos.CreateConsumableRequest;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableDtos.UpdateConsumableRequest;
import com.otapp.hmis.engine.masterdata.consumable.domain.Consumable;
import com.otapp.hmis.engine.masterdata.consumable.domain.ConsumableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsumableService {

    private final ConsumableRepository repo;

    @Transactional
    public ConsumableDto create(CreateConsumableRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Consumable code already exists: " + code);
        }
        Consumable c = new Consumable(code, request.name().trim(),
                request.unitOfMeasure(), request.description());
        repo.save(c);
        return toDto(c);
    }

    @Transactional
    public ConsumableDto update(String uid, UpdateConsumableRequest request) {
        Consumable c = loadOrThrow(uid);
        c.setName(request.name().trim());
        c.setUnitOfMeasure(request.unitOfMeasure());
        c.setDescription(request.description());
        return toDto(c);
    }

    @Transactional
    public ConsumableDto setActive(String uid, boolean active) {
        Consumable c = loadOrThrow(uid);
        if (active) c.activate(); else c.deactivate();
        return toDto(c);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public ConsumableDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<ConsumableDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable)
                        .map(ConsumableService::toDto));
    }

    private Consumable loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Consumable not found: " + uid));
    }

    private static ConsumableDto toDto(Consumable c) {
        return new ConsumableDto(c.getUid(), c.getCode(), c.getName(),
                c.getUnitOfMeasure(), c.getDescription(),
                c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
