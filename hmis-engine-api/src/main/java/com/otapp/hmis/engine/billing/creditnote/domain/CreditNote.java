package com.otapp.hmis.engine.billing.creditnote.domain;

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
 * A write-down raised against an invoice. The amount is positive — applied
 * via {@code Invoice.applyCreditNote(amount)} which adds it to
 * {@code totalCredited}, so the patient owes {@code subtotal -
 * totalPaid - totalCredited} after the credit lands.
 *
 * <p>Each credit note is its own immutable accounting record. Cancelling
 * an erroneously-raised credit note is done by raising a counter-credit
 * (negative-meaning reason like ERROR_CORRECTION). The entity itself
 * is never mutated after creation.
 */
@Entity
@Table(name = "credit_note",
       uniqueConstraints = @UniqueConstraint(name = "uk_credit_note_no", columnNames = "note_no"),
       indexes = {
               @Index(name = "idx_credit_note_invoice", columnList = "invoice_uid"),
               @Index(name = "idx_credit_note_issued",  columnList = "issued_at"),
               @Index(name = "idx_credit_note_reason",  columnList = "reason")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditNote extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_no", nullable = false, length = 32)
    private String noteNo;

    @Column(name = "invoice_uid", nullable = false, length = 26) private String invoiceUid;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CreditNoteReason reason;

    @Column(length = 500) private String description;

    @Column(name = "issued_by_username", nullable = false, length = 64) private String issuedByUsername;
    @Column(name = "issued_at",          nullable = false)              private Instant issuedAt;

    public CreditNote(String noteNo, String invoiceUid, BigDecimal amount, String currency,
                      CreditNoteReason reason, String description, String issuedByUsername) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Credit note amount must be positive");
        }
        if (reason == null) {
            throw new BusinessRuleException("Credit note reason is required");
        }
        this.noteNo = noteNo;
        this.invoiceUid = invoiceUid;
        this.amount = amount;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency;
        this.reason = reason;
        this.description = description;
        this.issuedByUsername = issuedByUsername;
        this.issuedAt = Instant.now();
    }
}
