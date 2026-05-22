package com.otapp.hmis.engine.hr.employee.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HR profile for a person who works at the hospital (PROCESS.md §12,
 * §17.11). Separate from {@code User} — not every employee has system
 * access (porters, cleaners), and not every system user is an employee
 * (the ROOT bootstrap account).
 *
 * <p>{@link #username} is an optional link to the {@code iam.User} account
 * for staff who log in. Unique when set so an account can't be claimed
 * by two HR profiles.
 *
 * <p>Designation + department are intentionally string fields for V1 —
 * upgrading them to masterdata aggregates is a future polish phase.
 */
@Entity
@Table(name = "hr_employee",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_hr_employee_employee_no", columnNames = "employee_no"),
               @UniqueConstraint(name = "uk_hr_employee_username",    columnNames = "username")
       },
       indexes = {
               @Index(name = "idx_hr_employee_status",      columnList = "employment_status"),
               @Index(name = "idx_hr_employee_designation", columnList = "designation"),
               @Index(name = "idx_hr_employee_department",  columnList = "department")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable employee number, e.g. EMP-2026-000123. */
    @Column(name = "employee_no", nullable = false, length = 32)
    private String employeeNo;

    // ----- person ---------------------------------------------------------

    @Setter @Column(name = "first_name",  nullable = false, length = 80) private String firstName;
    @Setter @Column(name = "middle_name", length = 80)                   private String middleName;
    @Setter @Column(name = "last_name",   nullable = false, length = 80) private String lastName;
    @Setter @Column(length = 16)                                          private String gender;
    @Setter @Column(name = "date_of_birth")                               private LocalDate dateOfBirth;
    @Setter @Column(name = "national_id", length = 32)                    private String nationalId;
    @Setter @Column(length = 32)                                          private String phone;
    @Setter @Column(length = 120)                                         private String email;
    @Setter @Column(length = 255)                                         private String address;

    // ----- employment -----------------------------------------------------

    /** Optional link to an iam.User. Null = no system access. */
    @Setter @Column(length = 64)
    private String username;

    @Setter @Column(length = 120) private String designation;
    @Setter @Column(length = 120) private String department;

    @Setter @Column(name = "hire_date", nullable = false) private LocalDate hireDate;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 16)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Setter @Column(name = "termination_date")                  private LocalDate terminationDate;
    @Setter @Column(name = "termination_reason", length = 500)  private String terminationReason;

    public Employee(String employeeNo, String firstName, String lastName,
                    LocalDate hireDate, String designation, String department) {
        if (firstName == null || firstName.isBlank()) {
            throw new BusinessRuleException("firstName is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new BusinessRuleException("lastName is required");
        }
        if (hireDate == null) {
            throw new BusinessRuleException("hireDate is required");
        }
        this.employeeNo = employeeNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.hireDate = hireDate;
        this.designation = designation;
        this.department = department;
    }

    public String fullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) {
            sb.append(' ').append(middleName);
        }
        sb.append(' ').append(lastName);
        return sb.toString();
    }

    public void terminate(LocalDate date, String reason) {
        if (employmentStatus == EmploymentStatus.TERMINATED) {
            throw new BusinessRuleException("Employee is already terminated");
        }
        if (date == null) {
            throw new BusinessRuleException("Termination date is required");
        }
        if (date.isBefore(hireDate)) {
            throw new BusinessRuleException("Termination date cannot precede hire date");
        }
        this.employmentStatus = EmploymentStatus.TERMINATED;
        this.terminationDate = date;
        this.terminationReason = reason;
    }
}
