package com.otapp.hmis.engine.hr.payroll.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.BandRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputedLineDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputePayrollRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputedPayrollDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.CreatePayrollComponentRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.PayrollComponentBandDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.PayrollComponentDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.UpdatePayrollComponentRequest;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollCalcBase;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollCalcMethod;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponent;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentBand;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentBandRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for configurable payroll components + the stateless auto-prefill
 * compute. All rates/bands are data — the system hard-codes no statutory
 * values; the hospital's HR team configures the actual figures.
 */
@Service
@RequiredArgsConstructor
public class PayrollComponentService {

    private static final int MONEY_SCALE = 2;

    private final PayrollComponentRepository componentRepository;
    private final PayrollComponentBandRepository bandRepository;

    // ----- CRUD --------------------------------------------------------------

    @Transactional
    public PayrollComponentDto create(CreatePayrollComponentRequest request) {
        if (componentRepository.existsByCode(request.code().trim())) {
            throw new BusinessRuleException("Component code already in use: " + request.code());
        }
        PayrollComponent component = new PayrollComponent(
                request.code(), request.name(), request.type(), request.method(), request.base());
        component.setFixedAmount(request.fixedAmount());
        component.setPercentRate(request.percentRate());
        component.setActive(request.active() == null || request.active());
        component.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        component.validateScalars();
        componentRepository.save(component);

        replaceBands(component, request.bands());
        return toDto(component);
    }

    @Transactional
    public PayrollComponentDto update(String uid, UpdatePayrollComponentRequest request) {
        PayrollComponent component = loadOrThrow(uid);
        component.setName(request.name());
        component.setType(request.type());
        component.setMethod(request.method());
        component.setBase(request.base() == null ? PayrollCalcBase.BASIC : request.base());
        component.setFixedAmount(request.fixedAmount());
        component.setPercentRate(request.percentRate());
        component.setActive(request.active());
        component.setSortOrder(request.sortOrder() == null ? component.getSortOrder() : request.sortOrder());
        component.validateScalars();

        bandRepository.deleteByComponentUid(component.getUid());
        replaceBands(component, request.bands());
        return toDto(component);
    }

    @Transactional
    public PayrollComponentDto setActive(String uid, boolean active) {
        PayrollComponent component = loadOrThrow(uid);
        component.setActive(active);
        return toDto(component);
    }

    @Transactional
    public void delete(String uid) {
        PayrollComponent component = loadOrThrow(uid);
        bandRepository.deleteByComponentUid(component.getUid());
        componentRepository.delete(component);
    }

    @Transactional(readOnly = true)
    public PayrollComponentDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<PayrollComponentDto> search(Boolean active, PayrollComponentType type, Pageable pageable) {
        return PageResponse.from(componentRepository.search(active, type, pageable).map(this::toDto));
    }

    // ----- compute (auto-prefill) -------------------------------------------

    @Transactional(readOnly = true)
    public ComputedPayrollDto compute(ComputePayrollRequest request) {
        BigDecimal basic = money(request.basicSalary());
        BigDecimal effectiveBasic = prorate(basic, request.workedDays(), request.periodDays());

        List<PayrollComponent> active = componentRepository.findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();
        Map<String, List<PayrollComponentBand>> bandsByComponent = active.isEmpty()
                ? Map.of()
                : bandRepository.findAllByComponentUidInOrderBySortOrderAsc(
                        active.stream().map(PayrollComponent::getUid).toList()).stream()
                        .collect(Collectors.groupingBy(PayrollComponentBand::getComponentUid));

        List<ComputedLineDto> lines = new ArrayList<>();

        // 1. Earnings first — always computed on the (pro-rated) basic.
        BigDecimal totalEarnings = BigDecimal.ZERO.setScale(MONEY_SCALE);
        for (PayrollComponent c : active) {
            if (c.getType() != PayrollComponentType.EARNING) continue;
            BigDecimal amount = amountFor(c, bandsByComponent.get(c.getUid()), effectiveBasic);
            totalEarnings = totalEarnings.add(amount);
            lines.add(line(c, amount));
        }
        BigDecimal gross = effectiveBasic.add(totalEarnings);

        // 2. Deductions — base is BASIC or GROSS per component.
        BigDecimal totalDeductions = BigDecimal.ZERO.setScale(MONEY_SCALE);
        for (PayrollComponent c : active) {
            if (c.getType() != PayrollComponentType.DEDUCTION) continue;
            BigDecimal baseVal = c.getBase() == PayrollCalcBase.GROSS ? gross : effectiveBasic;
            BigDecimal amount = amountFor(c, bandsByComponent.get(c.getUid()), baseVal);
            totalDeductions = totalDeductions.add(amount);
            lines.add(line(c, amount));
        }

        BigDecimal net = gross.subtract(totalDeductions);

        // 3. Employer contributions — employer-side cost (base BASIC or GROSS
        //    like deductions) tracked SEPARATELY. NOT added to gross, NOT
        //    subtracted from net (legacy PayrollDetail.employerContributions).
        BigDecimal totalEmployerContributions = BigDecimal.ZERO.setScale(MONEY_SCALE);
        for (PayrollComponent c : active) {
            if (c.getType() != PayrollComponentType.EMPLOYER_CONTRIBUTION) continue;
            BigDecimal baseVal = c.getBase() == PayrollCalcBase.GROSS ? gross : effectiveBasic;
            BigDecimal amount = amountFor(c, bandsByComponent.get(c.getUid()), baseVal);
            totalEmployerContributions = totalEmployerContributions.add(amount);
            lines.add(line(c, amount));
        }

        return new ComputedPayrollDto(
                basic, effectiveBasic, request.workedDays(), request.periodDays(),
                totalEarnings, gross, totalDeductions, net, totalEmployerContributions, lines);
    }

    // ----- helpers -----------------------------------------------------------

    /** Resolve a component's amount against a base value, rounded to money scale. */
    private BigDecimal amountFor(PayrollComponent c, List<PayrollComponentBand> bands, BigDecimal baseVal) {
        return switch (c.getMethod()) {
            case FIXED   -> money(c.getFixedAmount() == null ? BigDecimal.ZERO : c.getFixedAmount());
            case PERCENT -> money(baseVal.multiply(c.getPercentRate() == null ? BigDecimal.ZERO : c.getPercentRate()));
            case BAND    -> money(bandedAmount(bands, baseVal));
        };
    }

    /** Progressive band sum: each band's rate applies only to its slice of the base. */
    private static BigDecimal bandedAmount(List<PayrollComponentBand> bands, BigDecimal baseVal) {
        if (bands == null || bands.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (PayrollComponentBand band : bands) {
            BigDecimal lower = band.getFromAmount();
            if (baseVal.compareTo(lower) <= 0) continue;
            BigDecimal upper = band.getToAmount() == null ? baseVal : band.getToAmount().min(baseVal);
            BigDecimal slice = upper.subtract(lower);
            if (slice.signum() > 0) {
                total = total.add(slice.multiply(band.getRate()));
            }
        }
        return total;
    }

    private static BigDecimal prorate(BigDecimal basic, Integer workedDays, Integer periodDays) {
        if (workedDays == null || periodDays == null || periodDays <= 0) {
            return basic;
        }
        int worked = Math.min(workedDays, periodDays); // never pay more than a full period
        return money(basic.multiply(BigDecimal.valueOf(worked))
                .divide(BigDecimal.valueOf(periodDays), 6, RoundingMode.HALF_UP));
    }

    private void replaceBands(PayrollComponent component, List<BandRequest> bands) {
        if (component.getMethod() != PayrollCalcMethod.BAND) {
            return; // FIXED/PERCENT carry no bands
        }
        if (bands == null || bands.isEmpty()) {
            throw new BusinessRuleException("BAND component requires at least one band row");
        }
        List<BandRequest> ordered = bands.stream()
                .sorted(Comparator.comparing(BandRequest::fromAmount))
                .toList();
        int i = 0;
        for (BandRequest b : ordered) {
            bandRepository.save(new PayrollComponentBand(
                    component.getUid(), i++, b.fromAmount(), b.toAmount(), b.rate()));
        }
    }

    private PayrollComponent loadOrThrow(String uid) {
        return componentRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Payroll component not found: " + uid));
    }

    private PayrollComponentDto toDto(PayrollComponent c) {
        List<PayrollComponentBandDto> bands = bandRepository
                .findAllByComponentUidOrderBySortOrderAsc(c.getUid()).stream()
                .map(b -> new PayrollComponentBandDto(b.getUid(), b.getSortOrder(),
                        b.getFromAmount(), b.getToAmount(), b.getRate()))
                .toList();
        return new PayrollComponentDto(
                c.getUid(), c.getCode(), c.getName(), c.getType(), c.getMethod(), c.getBase(),
                c.getFixedAmount(), c.getPercentRate(), c.isActive(), c.getSortOrder(),
                bands, c.getCreatedAt(), c.getUpdatedAt());
    }

    private static ComputedLineDto line(PayrollComponent c, BigDecimal amount) {
        return new ComputedLineDto(c.getUid(), c.getCode(), c.getName(), c.getType(), c.getMethod(), amount);
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
