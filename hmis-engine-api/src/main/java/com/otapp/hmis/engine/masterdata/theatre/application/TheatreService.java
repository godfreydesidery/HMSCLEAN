package com.otapp.hmis.engine.masterdata.theatre.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreDtos.CreateTheatreRequest;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreDtos.TheatreDto;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreDtos.UpdateTheatreRequest;
import com.otapp.hmis.engine.masterdata.theatre.domain.Theatre;
import com.otapp.hmis.engine.masterdata.theatre.domain.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TheatreService {

    private final TheatreRepository repo;

    @Transactional
    public TheatreDto create(CreateTheatreRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Theatre code already exists: " + code);
        }
        Theatre t = new Theatre(code, request.name().trim(), request.location(), request.description());
        repo.save(t);
        return toDto(t);
    }

    @Transactional
    public TheatreDto update(String uid, UpdateTheatreRequest request) {
        Theatre t = loadOrThrow(uid);
        t.setName(request.name().trim());
        t.setLocation(request.location());
        t.setDescription(request.description());
        return toDto(t);
    }

    @Transactional
    public TheatreDto setActive(String uid, boolean active) {
        Theatre t = loadOrThrow(uid);
        if (active) t.activate(); else t.deactivate();
        return toDto(t);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public TheatreDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<TheatreDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable)
                        .map(TheatreService::toDto));
    }

    private Theatre loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Theatre not found: " + uid));
    }

    private static TheatreDto toDto(Theatre t) {
        return new TheatreDto(t.getUid(), t.getCode(), t.getName(),
                t.getLocation(), t.getDescription(),
                t.isActive(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
