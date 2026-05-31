package com.otapp.hmis.engine.hr.payroll.application;

import com.otapp.hmis.engine.hr.payroll.domain.PayrollCalcBase;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollCalcMethod;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PayrollComponentDtos {

    private PayrollComponentDtos() {}

    public record PayrollComponentBandDto(
            String uid,
            int sortOrder,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal rate) {}

    public record PayrollComponentDto(
            String uid,
            String code,
            String name,
            PayrollComponentType type,
            PayrollCalcMethod method,
            PayrollCalcBase base,
            BigDecimal fixedAmount,
            BigDecimal percentRate,
            boolean active,
            int sortOrder,
            List<PayrollComponentBandDto> bands,
            Instant createdAt,
            Instant updatedAt) {}

    public record BandRequest(
            @NotNull @DecimalMin("0.00") BigDecimal fromAmount,
            @DecimalMin("0.00") BigDecimal toAmount,
            @NotNull @DecimalMin("0.000000") BigDecimal rate) {}

    public record CreatePayrollComponentRequest(
            @NotBlank @Size(max = 32)  String code,
            @NotBlank @Size(max = 120) String name,
            @NotNull PayrollComponentType type,
            @NotNull PayrollCalcMethod method,
            PayrollCalcBase base,
            @DecimalMin("0.00") BigDecimal fixedAmount,
            @DecimalMin("0.000000") BigDecimal percentRate,
            Boolean active,
            @PositiveOrZero Integer sortOrder,
            @Valid List<BandRequest> bands) {}

    public record UpdatePayrollComponentRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull PayrollComponentType type,
            @NotNull PayrollCalcMethod method,
            PayrollCalcBase base,
            @DecimalMin("0.00") BigDecimal fixedAmount,
            @DecimalMin("0.000000") BigDecimal percentRate,
            @NotNull Boolean active,
            @PositiveOrZero Integer sortOrder,
            @Valid List<BandRequest> bands) {}

    /**
     * Stateless auto-prefill input. {@code basicSalary} is the employee's
     * basic pay; when {@code workedDays}/{@code periodDays} are both given the
     * basic is pro-rated to days actually worked before components apply.
     */
    public record ComputePayrollRequest(
            @NotNull @DecimalMin("0.00") BigDecimal basicSalary,
            @PositiveOrZero Integer workedDays,
            @PositiveOrZero Integer periodDays) {}

    public record SetActiveRequest(@NotNull Boolean active) {}

    public record ComputedLineDto(
            String componentUid,
            String code,
            String name,
            PayrollComponentType type,
            PayrollCalcMethod method,
            BigDecimal amount) {}

    public record ComputedPayrollDto(
            BigDecimal inputBasic,
            BigDecimal effectiveBasic,
            Integer workedDays,
            Integer periodDays,
            BigDecimal totalEarnings,
            BigDecimal grossPay,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            /** Employer-side cost; tracked, NOT in gross nor net. */
            BigDecimal totalEmployerContributions,
            List<ComputedLineDto> lines) {}
}
