package com.otapp.hmis.engine.masterdata.pricing.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServicePriceDto;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.SetServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.UpdateServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import java.math.BigDecimal;
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
    private final CurrencyService currencyService;

    /**
     * Create a new price for a (payer, service, currency) cell. Refuses with a
     * conflict if that exact cell already exists — the caller must edit the
     * existing row rather than overwrite it.
     */
    @Transactional
    public ServicePriceDto create(SetServicePriceRequest request) {
        String planUid = (request.planUid() == null || request.planUid().isBlank()) ? null : request.planUid();
        if (planUid != null && insurancePlanRepository.findByUid(planUid).isEmpty()) {
            throw new NotFoundException("Insurance plan not found: " + planUid);
        }
        if (!nameResolver.serviceExists(request.kind(), request.serviceUid())) {
            throw new NotFoundException(request.kind() + " service not found: " + request.serviceUid());
        }
        if (!currencyService.isActiveCode(request.currency())) {
            throw new BusinessRuleException("Unknown or inactive currency: " + request.currency());
        }
        validateBand(request.amount(), request.minAmount(), request.maxAmount());
        if (priceRepository.findCell(planUid, request.kind(), request.serviceUid(), request.currency()).isPresent()) {
            throw new ConflictException(
                    "A " + request.currency() + " price already exists for this payer and service — edit it instead.");
        }

        ServicePrice price = new ServicePrice(planUid, request.kind(), request.serviceUid(),
                request.amount(), request.currency(), request.note());
        price.setMinAmount(request.minAmount());
        price.setMaxAmount(request.maxAmount());
        priceRepository.save(price);

        return toDto(price);
    }

    /**
     * Update an existing price (by uid). Only amount / band / note change; the
     * key (payer, service, currency) is immutable.
     */
    @Transactional
    public ServicePriceDto update(String uid, UpdateServicePriceRequest request) {
        ServicePrice price = priceRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Price not found: " + uid));
        validateBand(request.amount(), request.minAmount(), request.maxAmount());
        price.setAmount(request.amount());
        price.setMinAmount(request.minAmount());
        price.setMaxAmount(request.maxAmount());
        price.setNote(request.note());
        return toDto(price);
    }

    /** A negotiable band must satisfy {@code min <= amount <= max} for whichever bounds are present. */
    private static void validateBand(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessRuleException("Min price (" + min + ") cannot exceed max price (" + max + ")");
        }
        if (min != null && amount.compareTo(min) < 0) {
            throw new BusinessRuleException("Price (" + amount + ") cannot be below min price (" + min + ")");
        }
        if (max != null && amount.compareTo(max) > 0) {
            throw new BusinessRuleException("Price (" + amount + ") cannot exceed max price (" + max + ")");
        }
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
                p.getMinAmount(),
                p.getMaxAmount(),
                p.getCurrency(),
                p.getNote(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
