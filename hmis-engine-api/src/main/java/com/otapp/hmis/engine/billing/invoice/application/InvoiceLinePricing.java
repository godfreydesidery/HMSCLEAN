package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pricing rules that apply to a single {@link InvoiceLine}: the negotiable
 * band ({@code [min, max]}) that comes from the service-price matrix, whether
 * a line's unit price may still be renegotiated, and validation of a proposed
 * override against that band. Shared by {@link InvoiceDtoAssembler} (to surface
 * the band) and {@link InvoiceService} (to enforce it).
 */
@Component
@RequiredArgsConstructor
class InvoiceLinePricing {

    private final PriceLookup priceLookup;

    /**
     * The price band resolved for a line, honouring the invoice's insurance
     * plan and currency. Returns {@code null} when the line kind has no priced
     * catalogue (e.g. CONSUMABLE, which is billed at snapshot cost).
     */
    PriceLookup.Resolved bandFor(InvoiceLine line, String planUid, String currency) {
        ServiceKind kind = serviceKindFor(line.getKind());
        if (kind == null || line.getServiceUid() == null) {
            return null;
        }
        return priceLookup.resolve(kind, line.getServiceUid(), planUid, currency);
    }

    /** A price may be renegotiated only before any money has been taken or written down. */
    boolean overridable(Invoice invoice) {
        boolean openStatus = invoice.getStatus() == InvoiceStatus.DRAFT
                || invoice.getStatus() == InvoiceStatus.ISSUED;
        return openStatus && invoice.settledAmount().signum() == 0;
    }

    /** Throws unless {@code newUnitPrice} sits within the resolved band (open band = allow). */
    void validateOverride(BigDecimal newUnitPrice, PriceLookup.Resolved band) {
        if (band == null) {
            return;
        }
        BigDecimal min = band.minAmount();
        BigDecimal max = band.maxAmount();
        if (min != null && newUnitPrice.compareTo(min) < 0) {
            throw new BusinessRuleException("Price " + newUnitPrice + " is below the minimum of " + min);
        }
        if (max != null && newUnitPrice.compareTo(max) > 0) {
            throw new BusinessRuleException("Price " + newUnitPrice + " exceeds the maximum of " + max);
        }
    }

    static ServiceKind serviceKindFor(InvoiceLineKind kind) {
        return switch (kind) {
            case CONSULTATION -> ServiceKind.CONSULTATION;
            case LAB_TEST     -> ServiceKind.LAB_TEST;
            case PROCEDURE    -> ServiceKind.PROCEDURE;
            case RADIOLOGY    -> ServiceKind.RADIOLOGY;
            case MEDICINE     -> ServiceKind.MEDICINE;
            case WARD         -> ServiceKind.WARD;
            case REGISTRATION -> ServiceKind.REGISTRATION;
            case CONSUMABLE   -> null; // billed at snapshot cost, not from the price matrix
        };
    }
}
