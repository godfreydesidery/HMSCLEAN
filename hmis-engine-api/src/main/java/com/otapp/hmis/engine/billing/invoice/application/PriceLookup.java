package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the price for a (payer, service) pair in a target currency.
 *
 * <p>A cell may hold one row per currency. Resolution prefers the plan-specific
 * price over the cash price, and within each prefers the target currency, then
 * the system default currency, then any currency on file. Returns
 * {@link BigDecimal#ZERO} when nothing is defined — the UI surfaces this so the
 * user can set a price.
 */
@Component
@RequiredArgsConstructor
class PriceLookup {

    private final ServicePriceRepository priceRepository;
    private final CurrencyService currencyService;

    record Resolved(BigDecimal amount, BigDecimal minAmount, BigDecimal maxAmount, String currency) {
        static Resolved zero(String defaultCurrency) {
            return new Resolved(BigDecimal.ZERO, null, null, defaultCurrency);
        }

        static Resolved of(ServicePrice p) {
            return new Resolved(p.getAmount(), p.getMinAmount(), p.getMaxAmount(), p.getCurrency());
        }
    }

    Resolved resolve(ServiceKind kind, String serviceUid, String planUid, String targetCurrency) {
        String defaultCurrency = currencyService.defaultCode();
        String currency = (targetCurrency == null || targetCurrency.isBlank()) ? defaultCurrency : targetCurrency;

        if (planUid != null && !planUid.isBlank()) {
            Resolved planPrice = pick(planUid, kind, serviceUid, currency, defaultCurrency);
            if (planPrice != null) return planPrice;
        }
        Resolved cashPrice = pick(null, kind, serviceUid, currency, defaultCurrency);
        if (cashPrice != null) return cashPrice;

        return Resolved.zero(currency);
    }

    /** Target currency → system default → any currency on file; null if the cell has no price at all. */
    private Resolved pick(String planUid, ServiceKind kind, String serviceUid,
                          String currency, String defaultCurrency) {
        Optional<ServicePrice> exact = priceRepository.findCell(planUid, kind, serviceUid, currency);
        if (exact.isPresent()) return Resolved.of(exact.get());

        if (!currency.equals(defaultCurrency)) {
            Optional<ServicePrice> inDefault = priceRepository.findCell(planUid, kind, serviceUid, defaultCurrency);
            if (inDefault.isPresent()) return Resolved.of(inDefault.get());
        }

        List<ServicePrice> any = priceRepository.findCellAnyCurrency(planUid, kind, serviceUid);
        return any.isEmpty() ? null : Resolved.of(any.get(0));
    }
}
