package com.otapp.hmis.engine.hr.employee.application;

import com.otapp.hmis.engine.hr.employee.domain.EmploymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class EmployeeDtos {

    private EmployeeDtos() {}

    public record EmployeeDto(
            Long id,
            String uid,
            String employeeNo,
            String firstName,
            String middleName,
            String lastName,
            String fullName,
            String gender,
            LocalDate dateOfBirth,
            String nationalId,
            String phone,
            String email,
            String address,
            String username,
            String designation,
            String department,
            LocalDate hireDate,
            EmploymentStatus employmentStatus,
            LocalDate terminationDate,
            String terminationReason,
            // compensation / banking / statutory (legacy parity)
            BigDecimal basicSalary,
            String tinNo,
            String bankName,
            String bankAccountNo,
            String bankAccountName,
            String socialSecurityNo,
            String socialSecurityName,
            boolean payable,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 80)  String firstName,
            @Size(max = 80)            String middleName,
            @NotBlank @Size(max = 80)  String lastName,
            @Size(max = 16)            String gender,
            LocalDate dateOfBirth,
            @Size(max = 32)            String nationalId,
            @Size(max = 32)            String phone,
            @Size(max = 120)           String email,
            @Size(max = 255)           String address,
            @Size(max = 64)            String username,
            @Size(max = 120)           String designation,
            @Size(max = 120)           String department,
            @NotNull LocalDate         hireDate,
            // compensation / banking / statutory (all optional, additive)
            @DecimalMin("0.00") BigDecimal basicSalary,
            @Size(max = 32)  String tinNo,
            @Size(max = 120) String bankName,
            @Size(max = 40)  String bankAccountNo,
            @Size(max = 120) String bankAccountName,
            @Size(max = 40)  String socialSecurityNo,
            @Size(max = 120) String socialSecurityName,
            /** Defaults to true when null (matches legacy default). */
            Boolean payable) {}

    public record UpdateEmployeeRequest(
            @NotBlank @Size(max = 80)  String firstName,
            @Size(max = 80)            String middleName,
            @NotBlank @Size(max = 80)  String lastName,
            @Size(max = 16)            String gender,
            LocalDate dateOfBirth,
            @Size(max = 32)            String nationalId,
            @Size(max = 32)            String phone,
            @Size(max = 120)           String email,
            @Size(max = 255)           String address,
            @Size(max = 64)            String username,
            @Size(max = 120)           String designation,
            @Size(max = 120)           String department,
            // compensation / banking / statutory (all optional, additive)
            @DecimalMin("0.00") BigDecimal basicSalary,
            @Size(max = 32)  String tinNo,
            @Size(max = 120) String bankName,
            @Size(max = 40)  String bankAccountNo,
            @Size(max = 120) String bankAccountName,
            @Size(max = 40)  String socialSecurityNo,
            @Size(max = 120) String socialSecurityName,
            /** Defaults to true when null (matches legacy default). */
            Boolean payable) {}

    /**
     * Dedicated compensation update (companion to the generic PUT). Lets an
     * HR-comp authority set just the pay/bank/statutory fields without
     * touching the contact card. All optional except via @DecimalMin.
     */
    public record UpdateCompensationRequest(
            @DecimalMin("0.00") BigDecimal basicSalary,
            @Size(max = 32)  String tinNo,
            @Size(max = 120) String bankName,
            @Size(max = 40)  String bankAccountNo,
            @Size(max = 120) String bankAccountName,
            @Size(max = 40)  String socialSecurityNo,
            @Size(max = 120) String socialSecurityName,
            Boolean payable) {}

    public record SetStatusRequest(@NotNull EmploymentStatus status) {}

    public record TerminateEmployeeRequest(
            @NotNull LocalDate terminationDate,
            @Size(max = 500) String reason) {}

    public record ClinicianPerformanceDto(
            String username,
            LocalDate from,
            LocalDate to,
            long consultations,
            long admissions,
            long labOrders,
            long radiologyOrders,
            long procedureOrders) {}
}
