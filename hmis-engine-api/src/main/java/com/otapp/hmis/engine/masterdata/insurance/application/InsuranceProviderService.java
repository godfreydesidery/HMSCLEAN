package com.otapp.hmis.engine.masterdata.insurance.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderDtos.CreateInsuranceProviderRequest;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderDtos.InsuranceProviderDto;
import com.otapp.hmis.engine.masterdata.insurance.application.InsuranceProviderDtos.UpdateInsuranceProviderRequest;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProvider;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsuranceProviderService {

    private final InsuranceProviderRepository repo;

    @Transactional
    public InsuranceProviderDto create(CreateInsuranceProviderRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repo.existsByCode(code)) {
            throw new ConflictException("Insurance provider code already exists: " + code);
        }
        InsuranceProvider i = new InsuranceProvider(code, request.name().trim(),
                request.contactPerson(), request.phone(), request.email(),
                request.address(), request.description());
        repo.save(i);
        return toDto(i);
    }

    @Transactional
    public InsuranceProviderDto update(String uid, UpdateInsuranceProviderRequest request) {
        InsuranceProvider i = loadOrThrow(uid);
        i.setName(request.name().trim());
        i.setContactPerson(request.contactPerson());
        i.setPhone(request.phone());
        i.setEmail(request.email());
        i.setAddress(request.address());
        i.setDescription(request.description());
        return toDto(i);
    }

    @Transactional
    public InsuranceProviderDto setActive(String uid, boolean active) {
        InsuranceProvider i = loadOrThrow(uid);
        if (active) i.activate(); else i.deactivate();
        return toDto(i);
    }

    @Transactional
    public void delete(String uid) { repo.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public InsuranceProviderDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<InsuranceProviderDto> search(String query, Boolean active, Pageable pageable) {
        return PageResponse.from(
                repo.search(query == null ? null : query.trim(), active, pageable).map(InsuranceProviderService::toDto));
    }

    private InsuranceProvider loadOrThrow(String uid) {
        return repo.findByUid(uid).orElseThrow(() -> new NotFoundException("Insurance provider not found: " + uid));
    }

    private static InsuranceProviderDto toDto(InsuranceProvider i) {
        return new InsuranceProviderDto(i.getUid(), i.getCode(), i.getName(), i.getContactPerson(),
                i.getPhone(), i.getEmail(), i.getAddress(), i.getDescription(),
                i.isActive(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
