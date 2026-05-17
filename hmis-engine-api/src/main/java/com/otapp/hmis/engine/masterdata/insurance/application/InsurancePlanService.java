package com.otapp.hmis.engine.masterdata.insurance.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanDtos.CreateInsurancePlanRequest;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanDtos.InsurancePlanDto;
import com.otapp.hmis.engine.masterdata.insurance.application.InsurancePlanDtos.UpdateInsurancePlanRequest;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProvider;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsuranceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsurancePlanService {

    private final InsurancePlanRepository planRepository;
    private final InsuranceProviderRepository providerRepository;

    @Transactional
    public InsurancePlanDto create(CreateInsurancePlanRequest request) {
        String code = request.code().trim().toUpperCase();
        if (planRepository.existsByCode(code)) {
            throw new ConflictException("Plan code already exists: " + code);
        }
        InsuranceProvider provider = providerRepository.findByUid(request.providerUid())
                .orElseThrow(() -> new NotFoundException("Insurance provider not found: " + request.providerUid()));

        InsurancePlan plan = new InsurancePlan(code, request.name().trim(),
                provider.getUid(), request.description());
        plan.setCoversConsultation(request.coversConsultation());
        plan.setCoversLab(request.coversLab());
        plan.setCoversRadiology(request.coversRadiology());
        plan.setCoversProcedure(request.coversProcedure());
        plan.setCoversMedicine(request.coversMedicine());
        plan.setCoversAdmission(request.coversAdmission());
        planRepository.save(plan);

        return toDto(plan, provider.getName());
    }

    @Transactional
    public InsurancePlanDto update(String uid, UpdateInsurancePlanRequest request) {
        InsurancePlan plan = loadOrThrow(uid);
        plan.setName(request.name().trim());
        plan.setCoversConsultation(request.coversConsultation());
        plan.setCoversLab(request.coversLab());
        plan.setCoversRadiology(request.coversRadiology());
        plan.setCoversProcedure(request.coversProcedure());
        plan.setCoversMedicine(request.coversMedicine());
        plan.setCoversAdmission(request.coversAdmission());
        plan.setDescription(request.description());
        return toDto(plan);
    }

    @Transactional
    public InsurancePlanDto setActive(String uid, boolean active) {
        InsurancePlan plan = loadOrThrow(uid);
        if (active) plan.activate(); else plan.deactivate();
        return toDto(plan);
    }

    @Transactional
    public void delete(String uid) { planRepository.delete(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public InsurancePlanDto findByUid(String uid) { return toDto(loadOrThrow(uid)); }

    @Transactional(readOnly = true)
    public PageResponse<InsurancePlanDto> search(String query, Boolean active, String providerUid, Pageable pageable) {
        return PageResponse.from(
                planRepository.search(query == null ? null : query.trim(), active, providerUid, pageable)
                        .map(this::toDto));
    }

    private InsurancePlan loadOrThrow(String uid) {
        return planRepository.findByUid(uid).orElseThrow(() -> new NotFoundException("Insurance plan not found: " + uid));
    }

    private InsurancePlanDto toDto(InsurancePlan p) {
        String providerName = providerRepository.findByUid(p.getProviderUid())
                .map(InsuranceProvider::getName)
                .orElse(null);
        return toDto(p, providerName);
    }

    private static InsurancePlanDto toDto(InsurancePlan p, String providerName) {
        return new InsurancePlanDto(
                p.getUid(), p.getCode(), p.getName(),
                p.getProviderUid(), providerName,
                p.isCoversConsultation(), p.isCoversLab(), p.isCoversRadiology(),
                p.isCoversProcedure(), p.isCoversMedicine(), p.isCoversAdmission(),
                p.getDescription(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
