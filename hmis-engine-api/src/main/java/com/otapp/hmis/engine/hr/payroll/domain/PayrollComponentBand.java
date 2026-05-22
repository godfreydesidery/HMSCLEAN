package com.otapp.hmis.engine.hr.payroll.domain;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row of a progressive band table for a BAND {@link PayrollComponent}
 * (e.g. a PAYE bracket). The {@link #rate} applies only to the slice of the
 * base between {@link #fromAmount} (inclusive) and {@link #toAmount}
 * (exclusive; {@code null} = open-ended top band).
 */
@Entity
@Table(name = "hr_payroll_component_band",
       indexes = @Index(name = "idx_hr_payroll_band_component", columnList = "component_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollComponentBand extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "component_uid", nullable = false, length = 26)
    private String componentUid;

    @Column(name = "sort_order", nullable = false) private int sortOrder;

    @Column(name = "from_amount", nullable = false, precision = 14, scale = 2) private BigDecimal fromAmount;

    /** Null = open-ended top band. */
    @Column(name = "to_amount", precision = 14, scale = 2) private BigDecimal toAmount;

    @Column(nullable = false, precision = 9, scale = 6) private BigDecimal rate;

    public PayrollComponentBand(String componentUid, int sortOrder,
                                BigDecimal fromAmount, BigDecimal toAmount, BigDecimal rate) {
        if (fromAmount == null || fromAmount.signum() < 0) {
            throw new BusinessRuleException("Band fromAmount must be non-negative");
        }
        if (toAmount != null && toAmount.compareTo(fromAmount) <= 0) {
            throw new BusinessRuleException("Band toAmount must be greater than fromAmount");
        }
        if (rate == null || rate.signum() < 0) {
            throw new BusinessRuleException("Band rate must be non-negative");
        }
        this.componentUid = componentUid;
        this.sortOrder = sortOrder;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.rate = rate;
    }
}
