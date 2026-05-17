package com.otapp.hmis.engine.masterdata.pharmacy.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyDtos.CreatePharmacyRequest;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyDtos.PharmacyDto;
import com.otapp.hmis.engine.masterdata.pharmacy.application.PharmacyDtos.UpdatePharmacyRequest;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    @Transactional
    public PharmacyDto create(CreatePharmacyRequest request) {
        String code = request.code().trim().toUpperCase();
        if (pharmacyRepository.existsByCode(code)) {
            throw new ConflictException("Pharmacy code already exists: " + code);
        }
        Pharmacy p = new Pharmacy(code, request.name().trim(), request.location(), request.description());
        pharmacyRepository.save(p);
        return toDto(p);
    }

    @Transactional
    public PharmacyDto update(String uid, UpdatePharmacyRequest request) {
        Pharmacy p = loadOrThrow(uid);
        p.setName(request.name().trim());
        p.setLocation(request.location());
        p.setDescription(request.description());
        return toDto(p);
    }

    @Transactional
    public PharmacyDto setActive(String uid, boolean active) {
        Pharmacy p = loadOrThrow(uid);
        if (active) p.activate(); else p.deactivate();
        return toDto(p);
    }

    @Transactional
    public void delete(String uid) {
        pharmacyRepository.delete(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PharmacyDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<PharmacyDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                pharmacyRepository.search(query == null ? null : query.trim(), active, pageable).map(PharmacyService::toDto));
    }

    private Pharmacy loadOrThrow(String uid) {
        return pharmacyRepository.findByUid(uid).orElseThrow(() -> new NotFoundException("Pharmacy not found: " + uid));
    }

    private static PharmacyDto toDto(Pharmacy p) {
        return new PharmacyDto(p.getUid(), p.getCode(), p.getName(), p.getLocation(),
                p.getDescription(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
