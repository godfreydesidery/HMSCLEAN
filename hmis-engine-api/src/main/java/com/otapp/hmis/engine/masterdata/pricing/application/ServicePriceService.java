package com.otapp.hmis.engine.masterdata.pricing.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServiceCoverageDto;
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

    private static final String PRICE_NOT_FOUND = "Price not found: ";

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
        validateCoverage(planUid, request.covered(), request.amount());
        if (priceRepository.findCell(planUid, request.kind(), request.serviceUid(), request.currency()).isPresent()) {
            throw new ConflictException(
                    "A " + request.currency() + " price already exists for this payer and service — edit it instead.");
        }

        ServicePrice price = new ServicePrice(planUid, request.kind(), request.serviceUid(),
                request.amount(), request.currency(), request.note());
        price.setMinAmount(request.minAmount());
        price.setMaxAmount(request.maxAmount());
        // Coverage applies only to plan rows; cash rows stay uncovered.
        price.setCovered(planUid != null && request.covered());
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
                .orElseThrow(() -> new NotFoundException(PRICE_NOT_FOUND + uid));
        validateBand(request.amount(), request.minAmount(), request.maxAmount());
        // Legacy update_*_price_by_insurance: amount == 0 auto-unsets coverage;
        // coverage requested with amount <= 0 is rejected.
        boolean covered = price.getPlanUid() != null && request.covered();
        if (request.amount().signum() == 0) {
            covered = false;
        }
        validateCoverage(price.getPlanUid(), covered, request.amount());
        price.setAmount(request.amount());
        price.setMinAmount(request.minAmount());
        price.setMaxAmount(request.maxAmount());
        price.setNote(request.note());
        price.setCovered(covered);
        return toDto(price);
    }

    /**
     * Cover a service for its plan (legacy {@code change_*_coverage} with
     * covered=true). Refuses an unpriced cell — coverage requires {@code amount > 0}.
     * 404 if the cell is unknown; the cell must be a plan row (cash cannot cover).
     */
    @Transactional
    public ServicePriceDto cover(String uid) {
        return setCovered(uid, true);
    }

    /** Uncover a service for its plan (legacy {@code change_*_coverage} with covered=false). */
    @Transactional
    public ServicePriceDto uncover(String uid) {
        return setCovered(uid, false);
    }

    private ServicePriceDto setCovered(String uid, boolean covered) {
        ServicePrice price = priceRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException(PRICE_NOT_FOUND + uid));
        if (covered && price.getPlanUid() == null) {
            throw new BusinessRuleException("A cash price cannot be marked as covered — coverage applies to plan prices only.");
        }
        validateCoverage(price.getPlanUid(), covered, price.getAmount());
        price.setCovered(covered);
        return toDto(price);
    }

    /**
     * Coverage gate (legacy {@code change_*_coverage}): a covered service must
     * carry a positive price. Rule/gate violation -> 422.
     */
    private static void validateCoverage(String planUid, boolean covered, BigDecimal amount) {
        if (planUid != null && covered && amount.signum() <= 0) {
            throw new BusinessRuleException(
                    "Could not change coverage. Invalid price value. Should not be equal or less than zero.");
        }
    }

    /**
     * The per-plan coverage grid: the plan's price rows, projected as coverage
     * rows (price + covered flag). 404 if the plan is unknown.
     */
    @Transactional(readOnly = true)
    public PageResponse<ServiceCoverageDto> coverageGrid(String planUid, ServiceKind kind, String query,
                                                         Pageable pageable) {
        if (planUid == null || planUid.isBlank() || insurancePlanRepository.findByUid(planUid).isEmpty()) {
            throw new NotFoundException("Insurance plan not found: " + planUid);
        }
        String searchFilter = (query == null || query.isBlank()) ? null : query.trim();
        return PageResponse.from(
                priceRepository.search(planUid, false, kind, null, null, searchFilter, pageable)
                        .map(this::toCoverageDto));
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
                .orElseThrow(() -> new NotFoundException(PRICE_NOT_FOUND + uid));
        priceRepository.delete(price);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServicePriceDto> search(String planUid, boolean cashOnly, ServiceKind kind,
                                                String serviceUid, String currency, String query,
                                                Pageable pageable) {
        String planFilter = (planUid == null || planUid.isBlank()) ? null : planUid;
        String serviceFilter = (serviceUid == null || serviceUid.isBlank()) ? null : serviceUid;
        String currencyFilter = (currency == null || currency.isBlank()) ? null : currency.toUpperCase();
        String searchFilter = (query == null || query.isBlank()) ? null : query.trim();
        return PageResponse.from(
                priceRepository.search(planFilter, cashOnly, kind, serviceFilter, currencyFilter, searchFilter, pageable)
                        .map(this::toDto));
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
                p.isCovered(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private ServiceCoverageDto toCoverageDto(ServicePrice p) {
        String planName = p.getPlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(p.getPlanUid()).map(InsurancePlan::getName).orElse(null);
        return new ServiceCoverageDto(
                p.getId(),
                p.getUid(),
                p.getPlanUid(),
                planName,
                p.getKind(),
                p.getServiceUid(),
                nameResolver.resolveName(p.getKind(), p.getServiceUid()),
                p.getAmount(),
                p.getCurrency(),
                p.isCovered());
    }
}
