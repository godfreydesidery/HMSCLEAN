package com.otapp.hmis.engine.billing.refund.domain;

import com.otapp.hmis.engine.billing.payment.domain.PaymentMethod;
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

/**
 * Money returned to a patient against a previously-paid invoice
 * (PROCESS.md §11, §16). Amount is positive; applied via
 * {@code Invoice.applyRefund(amount)} which subtracts from
 * {@code totalPaid} and may roll the invoice status back to
 * PARTIALLY_PAID or ISSUED.
 *
 * <p>Immutable accounting record — to correct a refund, raise a
 * compensating payment.
 */
@Entity
@Table(name = "refund",
       uniqueConstraints = @UniqueConstraint(name = "uk_refund_no", columnNames = "refund_no"),
       indexes = {
               @Index(name = "idx_refund_invoice",  columnList = "invoice_uid"),
               @Index(name = "idx_refund_refunded", columnList = "refunded_at"),
               @Index(name = "idx_refund_reason",   columnList = "reason")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_no", nullable = false, length = 32)
    private String refundNo;

    @Column(name = "invoice_uid", nullable = false, length = 26) private String invoiceUid;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RefundReason reason;

    @Column(length = 500) private String description;
    @Column(name = "reference", length = 80) private String reference;

    @Column(name = "refunded_by_username", nullable = false, length = 64) private String refundedByUsername;
    @Column(name = "refunded_at",          nullable = false)              private Instant refundedAt;

    public Refund(String refundNo, String invoiceUid, BigDecimal amount, String currency,
                  PaymentMethod method, RefundReason reason,
                  String description, String reference, String refundedByUsername) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Refund amount must be positive");
        }
        if (method == null) {
            throw new BusinessRuleException("Refund method is required");
        }
        if (reason == null) {
            throw new BusinessRuleException("Refund reason is required");
        }
        this.refundNo = refundNo;
        this.invoiceUid = invoiceUid;
        this.amount = amount;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency;
        this.method = method;
        this.reason = reason;
        this.description = description;
        this.reference = reference;
        this.refundedByUsername = refundedByUsername;
        this.refundedAt = Instant.now();
    }
}
