package com.otapp.hmis.engine.masterdata.pricing.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServicePriceDto;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.SetServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicePriceService {

    private final ServicePriceRepository priceRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final ServiceNameResolver nameResolver;

    /**
     * Upsert: creates a new row if no price exists for (plan, kind, service),
     * otherwise updates amount / currency / note in place.
     */
    @Transactional
    public ServicePriceDto setPrice(SetServicePriceRequest request) {
        String planUid = (request.planUid() == null || request.planUid().isBlank()) ? null : request.planUid();
        if (planUid != null && insurancePlanRepository.findByUid(planUid).isEmpty()) {
            throw new NotFoundException("Insurance plan not found: " + planUid);
        }
        if (!nameResolver.serviceExists(request.kind(), request.serviceUid())) {
            throw new NotFoundException(request.kind() + " service not found: " + request.serviceUid());
        }

        ServicePrice price = priceRepository
                .findByPlanUidAndKindAndServiceUid(planUid, request.kind(), request.serviceUid())
                .orElseGet(() -> new ServicePrice(planUid, request.kind(), request.serviceUid(),
                        request.amount(), request.currency(), request.note()));

        price.setAmount(request.amount());
        price.setCurrency(request.currency());
        price.setNote(request.note());
        priceRepository.save(price);

        return toDto(price);
    }

    @Transactional
    public void delete(String uid) {
        ServicePrice price = priceRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Price not found: " + uid));
        priceRepository.delete(price);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServicePriceDto> search(String planUid, ServiceKind kind, String serviceUid, Pageable pageable) {
        String planFilter = (planUid == null || planUid.isBlank()) ? null : planUid;
        String serviceFilter = (serviceUid == null || serviceUid.isBlank()) ? null : serviceUid;
        return PageResponse.from(
                priceRepository.search(planFilter, kind, serviceFilter, pageable).map(this::toDto));
    }

    private ServicePriceDto toDto(ServicePrice p) {
        String planName = p.getPlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(p.getPlanUid()).map(InsurancePlan::getName).orElse(null);
        String serviceName = nameResolver.resolveName(p.getKind(), p.getServiceUid());
        return new ServicePriceDto(
                p.getUid(),
                p.getPlanUid(),
                planName,
                p.getKind(),
                p.getServiceUid(),
                serviceName,
                p.getAmount(),
                p.getCurrency(),
                p.getNote(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
