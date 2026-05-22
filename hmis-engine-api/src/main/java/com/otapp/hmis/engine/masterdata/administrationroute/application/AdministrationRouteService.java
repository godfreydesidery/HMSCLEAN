package com.otapp.hmis.engine.masterdata.administrationroute.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteDtos.AdministrationRouteDto;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteDtos.CreateAdministrationRouteRequest;
import com.otapp.hmis.engine.masterdata.administrationroute.application.AdministrationRouteDtos.UpdateAdministrationRouteRequest;
import com.otapp.hmis.engine.masterdata.administrationroute.domain.AdministrationRoute;
import com.otapp.hmis.engine.masterdata.administrationroute.domain.AdministrationRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdministrationRouteService {

    private final AdministrationRouteRepository repo;

    @Transactional
    public AdministrationRouteDto create(CreateAdministrationRouteRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Administration-route code already exists: " + code);
        }
        AdministrationRoute r = new AdministrationRoute(code, request.name().trim(), request.description());
        repo.save(r);
        return toDto(r);
    }

    @Transactional
    public AdministrationRouteDto update(String uid, UpdateAdministrationRouteRequest request) {
        AdministrationRoute r = loadOrThrow(uid);
        r.setName(request.name().trim());
        r.setDescription(request.description());
        return toDto(r);
    }

    @Transactional
    public AdministrationRouteDto setActive(String uid, boolean active) {
        AdministrationRoute r = loadOrThrow(uid);
        if (active) r.activate(); else r.deactivate();
        return toDto(r);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public AdministrationRouteDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<AdministrationRouteDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable)
                        .map(AdministrationRouteService::toDto));
    }

    private AdministrationRoute loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Administration route not found: " + uid));
    }

    private static AdministrationRouteDto toDto(AdministrationRoute r) {
        return new AdministrationRouteDto(r.getUid(), r.getCode(), r.getName(), r.getDescription(),
                r.isActive(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
