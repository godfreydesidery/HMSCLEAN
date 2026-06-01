package com.otapp.hmis.engine.masterdata.externalprovider.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderDtos.CreateExternalMedicalProviderRequest;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderDtos.ExternalMedicalProviderDto;
import com.otapp.hmis.engine.masterdata.externalprovider.application.ExternalMedicalProviderDtos.UpdateExternalMedicalProviderRequest;
import com.otapp.hmis.engine.masterdata.externalprovider.domain.ExternalMedicalProvider;
import com.otapp.hmis.engine.masterdata.externalprovider.domain.ExternalMedicalProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExternalMedicalProviderService {

    private final ExternalMedicalProviderRepository repo;

    @Transactional
    public ExternalMedicalProviderDto create(CreateExternalMedicalProviderRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("External provider code already exists: " + code);
        }
        ExternalMedicalProvider p = new ExternalMedicalProvider(code, request.name().trim());
        applyEditable(p, request.address(), request.telephone(), request.email(), request.fax(), request.website());
        repo.save(p);
        return toDto(p);
    }

    @Transactional
    public ExternalMedicalProviderDto update(String uid, UpdateExternalMedicalProviderRequest request) {
        ExternalMedicalProvider p = loadOrThrow(uid);
        p.setName(request.name().trim());
        applyEditable(p, request.address(), request.telephone(), request.email(), request.fax(), request.website());
        return toDto(p);
    }

    @Transactional
    public ExternalMedicalProviderDto setActive(String uid, boolean active) {
        ExternalMedicalProvider p = loadOrThrow(uid);
        if (active) p.activate(); else p.deactivate();
        return toDto(p);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public ExternalMedicalProviderDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<ExternalMedicalProviderDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable)
                        .map(ExternalMedicalProviderService::toDto));
    }

    private ExternalMedicalProvider loadOrThrow(String uid) {
        return repo.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("External provider not found: " + uid));
    }

    private static void applyEditable(ExternalMedicalProvider p, String address, String telephone,
                                      String email, String fax, String website) {
        p.setAddress(emptyToNull(address));
        p.setTelephone(emptyToNull(telephone));
        p.setEmail(emptyToNull(email));
        p.setFax(emptyToNull(fax));
        p.setWebsite(emptyToNull(website));
    }

    private static ExternalMedicalProviderDto toDto(ExternalMedicalProvider p) {
        return new ExternalMedicalProviderDto(p.getUid(), p.getCode(), p.getName(),
                p.getAddress(), p.getTelephone(), p.getEmail(), p.getFax(), p.getWebsite(),
                p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
