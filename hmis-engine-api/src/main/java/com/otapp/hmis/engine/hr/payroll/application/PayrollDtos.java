package com.otapp.hmis.engine.hr.payroll.application;

import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentType;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollPeriodStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PayrollDtos {

    private PayrollDtos() {}

    public record PayrollPeriodDto(
            String uid,
            String code,
            String label,
            LocalDate startDate,
            LocalDate endDate,
            String currency,
            PayrollPeriodStatus status,
            String note,
            Instant verifiedAt,
            String verifiedByUsername,
            Instant approvedAt,
            String approvedByUsername,
            Instant paidAt,
            Instant cancelledAt,
            String cancelReason,
            int itemCount,
            BigDecimal totalNet,
            Instant createdAt,
            Instant updatedAt) {}

    public record PayrollItemLineDto(
            String uid,
            String code,
            String name,
            PayrollComponentType type,
            BigDecimal amount,
            int sortOrder) {}

    public record PayrollItemDto(
            String uid,
            String periodUid,
            String employeeUid,
            String employeeNo,
            String employeeName,
            BigDecimal grossPay,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            String paymentMethod,
            String paymentReference,
            String note,
            List<PayrollItemLineDto> lines,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreatePayrollPeriodRequest(
            @NotBlank @Size(max = 32)  String code,
            @NotBlank @Size(max = 80)  String label,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Size(max = 500) String note) {}

    /** A single itemised line for the upsert — typically taken from the component compute. */
    public record PayrollItemLineRequest(
            @Size(max = 32)  String code,
            @NotBlank @Size(max = 120) String name,
            @NotNull PayrollComponentType type,
            @NotNull @DecimalMin("0.00") BigDecimal amount) {}

    public record UpsertPayrollItemRequest(
            @NotBlank @Size(min = 26, max = 26) String employeeUid,
            @NotNull @DecimalMin("0.00") BigDecimal grossPay,
            @NotNull @DecimalMin("0.00") BigDecimal totalDeductions,
            @Size(max = 32)  String paymentMethod,
            @Size(max = 80)  String paymentReference,
            @Size(max = 500) String note,
            /** Optional itemised breakdown (basic / allowances / PAYE / NHIF / loan …). */
            @Valid List<PayrollItemLineRequest> lines) {}

    public record CancelPayrollPeriodRequest(@Size(max = 500) String reason) {}

    public record PayrollPeriodWithItemsDto(
            PayrollPeriodDto period,
            List<PayrollItemDto> items) {}
}
