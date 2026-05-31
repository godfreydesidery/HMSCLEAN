package com.otapp.hmis.engine.masterdata.pricing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.currency.application.CurrencyService;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServicePriceDto;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.UpdateServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Unit coverage of the additive per-service coverage management (no Spring, no
 * DB). Faithful to legacy {@code InsurancePlanResource.change_*_coverage} /
 * {@code update_*_price_by_insurance}: covering a service requires a positive
 * price, setting the price to zero auto-unsets coverage, and a cash row cannot
 * be covered. Coverage rule violations are {@link BusinessRuleException} (422).
 */
@ExtendWith(MockitoExtension.class)
class ServicePriceCoverageTest {

    private static final String PLAN = "01PLAN0000000000000000000A";
    private static final String LAB = "01LAB00000000000000000000A";
    private static final String CCY = "TZS";

    @Mock private ServicePriceRepository priceRepository;
    @Mock private InsurancePlanRepository insurancePlanRepository;
    @Mock private ServiceNameResolver nameResolver;
    @Mock private CurrencyService currencyService;
    @InjectMocks private ServicePriceService service;

    private static ServicePrice planCell(BigDecimal amount, boolean covered) {
        ServicePrice p = new ServicePrice(PLAN, ServiceKind.LAB_TEST, LAB, amount, CCY, null);
        p.setCovered(covered);
        return p;
    }

    @Test
    void coveringAnUnpricedServiceIsRejected() {
        when(priceRepository.findByUid("X")).thenReturn(Optional.of(planCell(BigDecimal.ZERO, false)));

        assertThatThrownBy(() -> service.cover("X"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid price value");
    }

    @Test
    void coveringAPricedServiceSetsCovered() {
        ServicePrice priced = planCell(new BigDecimal("50.00"), false);
        when(priceRepository.findByUid("X")).thenReturn(Optional.of(priced));

        ServicePriceDto dto = service.cover("X");

        assertThat(priced.isCovered()).isTrue();
        assertThat(dto.covered()).isTrue();
    }

    @Test
    void cashPriceCannotBeCovered() {
        ServicePrice cash = new ServicePrice(null, ServiceKind.LAB_TEST, LAB, new BigDecimal("50.00"), CCY, null);
        when(priceRepository.findByUid("X")).thenReturn(Optional.of(cash));

        assertThatThrownBy(() -> service.cover("X"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cash price cannot be marked as covered");
    }

    @Test
    void updateToZeroAmountAutoUnsetsCoverage() {
        ServicePrice priced = planCell(new BigDecimal("50.00"), true);
        when(priceRepository.findByUid("X")).thenReturn(Optional.of(priced));

        ServicePriceDto dto = service.update("X",
                new UpdateServicePriceRequest(BigDecimal.ZERO, null, null, null, true));

        assertThat(priced.isCovered()).isFalse();
        assertThat(dto.covered()).isFalse();
    }

    @Test
    void uncoverClearsTheFlag() {
        ServicePrice priced = planCell(new BigDecimal("50.00"), true);
        when(priceRepository.findByUid("X")).thenReturn(Optional.of(priced));

        service.uncover("X");

        assertThat(priced.isCovered()).isFalse();
    }

    @Test
    void coverageGridRejectsUnknownPlan() {
        when(insurancePlanRepository.findByUid("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.coverageGrid("NOPE", null, null, Pageable.unpaged()))
                .isInstanceOf(NotFoundException.class);
    }
}
