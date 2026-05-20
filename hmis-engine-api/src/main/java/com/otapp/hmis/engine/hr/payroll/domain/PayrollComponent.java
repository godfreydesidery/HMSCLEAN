package com.otapp.hmis.engine.hr.payroll.domain;

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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable earning or deduction rule used to auto-prefill payroll
 * items. The hospital's HR team enters the actual rates/bands — the system
 * hard-codes no statutory values. {@link PayrollComponentBand} rows hold the
 * progressive table when {@link #method} is {@link PayrollCalcMethod#BAND}.
 */
@Entity
@Table(name = "hr_payroll_component",
       uniqueConstraints = @UniqueConstraint(name = "uk_hr_payroll_component_code", columnNames = "code"),
       indexes = {
               @Index(name = "idx_hr_payroll_component_active", columnList = "active"),
               @Index(name = "idx_hr_payroll_component_type",   columnList = "type")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollComponent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String code;

    @Setter @Column(nullable = false, length = 120) private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayrollComponentType type;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayrollCalcMethod method;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayrollCalcBase base = PayrollCalcBase.BASIC;

    /** FIXED only — the flat money amount. */
    @Setter @Column(name = "fixed_amount", precision = 14, scale = 2) private BigDecimal fixedAmount;

    /** PERCENT only — a fraction, e.g. 0.100000 = 10%. */
    @Setter @Column(name = "percent_rate", precision = 9, scale = 6) private BigDecimal percentRate;

    @Setter @Column(nullable = false) private boolean active = true;

    @Setter @Column(name = "sort_order", nullable = false) private int sortOrder = 0;

    public PayrollComponent(String code, String name, PayrollComponentType type,
                            PayrollCalcMethod method, PayrollCalcBase base) {
        if (code == null || code.isBlank())  throw new BusinessRuleException("code is required");
        if (name == null || name.isBlank())  throw new BusinessRuleException("name is required");
        if (type == null)   throw new BusinessRuleException("type is required");
        if (method == null) throw new BusinessRuleException("method is required");
        this.code = code.trim();
        this.name = name.trim();
        this.type = type;
        this.method = method;
        this.base = base == null ? PayrollCalcBase.BASIC : base;
    }

    /**
     * Validate the FIXED/PERCENT scalar matches the method. BAND components
     * are validated against their band rows in the service (which owns them).
     */
    public void validateScalars() {
        switch (method) {
            case FIXED -> {
                if (fixedAmount == null || fixedAmount.signum() < 0) {
                    throw new BusinessRuleException("FIXED component needs a non-negative fixed amount");
                }
            }
            case PERCENT -> {
                if (percentRate == null || percentRate.signum() < 0) {
                    throw new BusinessRuleException("PERCENT component needs a non-negative rate");
                }
            }
            case BAND -> { /* bands validated in the service */ }
        }
    }
}
