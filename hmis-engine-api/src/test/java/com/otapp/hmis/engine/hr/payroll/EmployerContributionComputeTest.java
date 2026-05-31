package com.otapp.hmis.engine.hr.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputePayrollRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputedPayrollDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentService;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollCalcBase;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollCalcMethod;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponent;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentBandRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentRepository;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentType;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit coverage of the additive EMPLOYER_CONTRIBUTION semantics (no Spring,
 * no DB). Asserts the employer-side cost is tracked SEPARATELY and never
 * touches gross or net — the legacy {@code PayrollDetail.employerContributions}
 * invariant — both in the compute engine and on the {@code PayrollItem} domain.
 */
@ExtendWith(MockitoExtension.class)
class EmployerContributionComputeTest {

    @Mock  private PayrollComponentRepository componentRepository;
    @Mock  private PayrollComponentBandRepository bandRepository;
    @InjectMocks private PayrollComponentService service;

    private static PayrollComponent component(String code, PayrollComponentType type,
                                              PayrollCalcMethod method, PayrollCalcBase base) {
        return new PayrollComponent(code, code, type, method, base);
    }

    @Test
    void employerContributionIsTrackedSeparatelyAndNeverAffectsGrossOrNet() {
        // EARNING 50,000 fixed; DEDUCTION 10% of gross; EMPLOYER_CONTRIBUTION 10% of basic.
        PayrollComponent housing = component("HOUSING", PayrollComponentType.EARNING,
                PayrollCalcMethod.FIXED, PayrollCalcBase.BASIC);
        housing.setFixedAmount(new BigDecimal("50000.00"));

        PayrollComponent pension = component("PENSION", PayrollComponentType.DEDUCTION,
                PayrollCalcMethod.PERCENT, PayrollCalcBase.GROSS);
        pension.setPercentRate(new BigDecimal("0.100000"));

        PayrollComponent employerMatch = component("EMP_NSSF", PayrollComponentType.EMPLOYER_CONTRIBUTION,
                PayrollCalcMethod.PERCENT, PayrollCalcBase.BASIC);
        employerMatch.setPercentRate(new BigDecimal("0.100000"));

        when(componentRepository.findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc())
                .thenReturn(List.of(housing, pension, employerMatch));
        when(bandRepository.findAllByComponentUidInOrderBySortOrderAsc(anyList()))
                .thenReturn(List.of());

        ComputedPayrollDto result = service.compute(
                new ComputePayrollRequest(new BigDecimal("600000.00"), null, null));

        // gross = basic 600,000 + earning 50,000 = 650,000 (employer cost NOT added).
        assertThat(result.grossPay()).isEqualByComparingTo("650000.00");
        // deductions = 10% of gross = 65,000 (employer cost NOT a deduction).
        assertThat(result.totalDeductions()).isEqualByComparingTo("65000.00");
        // net = gross - deductions = 585,000 (employer cost excluded).
        assertThat(result.netPay()).isEqualByComparingTo("585000.00");
        // employer contribution = 10% of basic 600,000 = 60,000, reported on its own.
        assertThat(result.totalEmployerContributions()).isEqualByComparingTo("60000.00");
        // It is emitted as a line but does not perturb the money identity above.
        assertThat(result.lines()).hasSize(3);
        assertThat(result.netPay())
                .as("net must equal gross minus deductions, ignoring employer cost")
                .isEqualByComparingTo(result.grossPay().subtract(result.totalDeductions()));
    }

    @Test
    void payrollItemNetExcludesEmployerContributions() {
        PayrollItem item = new PayrollItem("PERIOD1", "EMP1",
                new BigDecimal("1000000.00"), new BigDecimal("150000.00"));
        item.setEmployerContributions(new BigDecimal("90000.00"));
        item.recomputeNet();

        assertThat(item.getNetPay())
                .as("net = gross - deductions; employer contributions are not subtracted")
                .isEqualByComparingTo("850000.00");
        assertThat(item.getEmployerContributions()).isEqualByComparingTo("90000.00");
    }
}
