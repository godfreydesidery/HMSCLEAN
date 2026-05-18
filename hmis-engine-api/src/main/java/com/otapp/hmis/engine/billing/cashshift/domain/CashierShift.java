package com.otapp.hmis.engine.billing.cashshift.domain;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One cashier's open-to-close session. End-of-day reconciliation
 * (PROCESS.md §11) compares the declared closing float against the
 * expected total (opening + cash payments received during the window)
 * and surfaces the variance for audit.
 *
 * <p>Cashier-identity is the {@code iam.User} username — payments are
 * attributed via their {@code created_by} audit field. A user can only
 * have one OPEN shift at a time; opening a second one throws.
 */
@Entity
@Table(name = "cashier_shift",
       uniqueConstraints = @UniqueConstraint(name = "uk_cashier_shift_uid", columnNames = "uid"),
       indexes = {
               @Index(name = "idx_cashier_shift_user",      columnList = "cashier_username"),
               @Index(name = "idx_cashier_shift_status",    columnList = "status"),
               @Index(name = "idx_cashier_shift_opened_at", columnList = "opened_at"),
               @Index(name = "idx_cashier_shift_closed_at", columnList = "closed_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashierShift extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_username", nullable = false, length = 64)
    private String cashierUsername;

    @Column(nullable = false, length = 3) private String currency;

    @Column(name = "opening_float", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingFloat;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CashierShiftStatus status = CashierShiftStatus.OPEN;

    @Setter @Column(name = "closed_at") private Instant closedAt;
    @Setter @Column(name = "closing_declared_amount", precision = 14, scale = 2)
    private BigDecimal closingDeclaredAmount;
    @Setter @Column(name = "closing_expected_amount", precision = 14, scale = 2)
    private BigDecimal closingExpectedAmount;
    @Setter @Column(name = "variance", precision = 14, scale = 2)
    private BigDecimal variance;
    @Setter @Column(name = "closing_note", length = 500) private String closingNote;

    public CashierShift(String cashierUsername, String currency, BigDecimal openingFloat) {
        if (cashierUsername == null || cashierUsername.isBlank()) {
            throw new BusinessRuleException("cashierUsername is required");
        }
        if (openingFloat == null || openingFloat.signum() < 0) {
            throw new BusinessRuleException("openingFloat must be non-negative");
        }
        this.cashierUsername = cashierUsername;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency.toUpperCase();
        this.openingFloat = openingFloat;
        this.openedAt = Instant.now();
    }

    public void close(BigDecimal declared, BigDecimal expectedCashTakings, String note) {
        if (status != CashierShiftStatus.OPEN) {
            throw new BusinessRuleException("Shift is already closed");
        }
        if (declared == null || declared.signum() < 0) {
            throw new BusinessRuleException("Declared closing amount must be non-negative");
        }
        if (expectedCashTakings == null) {
            expectedCashTakings = BigDecimal.ZERO;
        }
        BigDecimal expectedTotal = openingFloat.add(expectedCashTakings);
        this.closedAt = Instant.now();
        this.closingDeclaredAmount = declared;
        this.closingExpectedAmount = expectedTotal;
        this.variance = declared.subtract(expectedTotal);
        this.closingNote = note;
        this.status = CashierShiftStatus.CLOSED;
    }
}
