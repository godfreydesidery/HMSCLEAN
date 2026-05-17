package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the price for a (plan, service) pair, falling back to the cash
 * price when there is no plan-specific entry. Returns {@link BigDecimal#ZERO}
 * if neither is defined — the UI surfaces this so the user can set a price.
 */
@Component
@RequiredArgsConstructor
class PriceLookup {

    private final ServicePriceRepository priceRepository;

    record Resolved(BigDecimal amount, String currency) {
        static Resolved zero(String defaultCurrency) {
            return new Resolved(BigDecimal.ZERO, defaultCurrency);
        }
    }

    Resolved resolve(ServiceKind kind, String serviceUid, String planUid, String defaultCurrency) {
        if (planUid != null && !planUid.isBlank()) {
            Optional<ServicePrice> planPrice = priceRepository.findByPlanUidAndKindAndServiceUid(planUid, kind, serviceUid);
            if (planPrice.isPresent()) {
                ServicePrice p = planPrice.get();
                return new Resolved(p.getAmount(), p.getCurrency());
            }
        }
        return priceRepository.findCashPrice(kind, serviceUid)
                .map(p -> new Resolved(p.getAmount(), p.getCurrency()))
                .orElseGet(() -> Resolved.zero(defaultCurrency));
    }
}
