package com.otapp.hmis.engine.masterdata.store.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.CreateStoreRequest;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.StoreDto;
import com.otapp.hmis.engine.masterdata.store.application.StoreDtos.UpdateStoreRequest;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreDto create(CreateStoreRequest request) {
        String code = request.code().trim().toUpperCase();
        if (storeRepository.existsByCode(code)) {
            throw new ConflictException("Store code already exists: " + code);
        }
        Store s = new Store(code, request.name().trim(), request.location(), request.description());
        storeRepository.save(s);
        return toDto(s);
    }

    @Transactional
    public StoreDto update(String uid, UpdateStoreRequest request) {
        Store s = loadOrThrow(uid);
        s.setName(request.name().trim());
        s.setLocation(request.location());
        s.setDescription(request.description());
        return toDto(s);
    }

    @Transactional
    public StoreDto setActive(String uid, boolean active) {
        Store s = loadOrThrow(uid);
        if (active) s.activate(); else s.deactivate();
        return toDto(s);
    }

    @Transactional
    public void delete(String uid) { storeRepository.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public StoreDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<StoreDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                storeRepository.search(query == null ? null : query.trim(), active, pageable).map(StoreService::toDto));
    }

    private Store loadOrThrow(String uid) {
        return storeRepository.findByUid(uid).orElseThrow(() -> new NotFoundException("Store not found: " + uid));
    }

    static StoreDto toDto(Store s) {
        return new StoreDto(s.getUid(), s.getCode(), s.getName(), s.getLocation(),
                s.getDescription(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
