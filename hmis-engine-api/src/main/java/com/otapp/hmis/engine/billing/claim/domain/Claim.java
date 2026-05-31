package com.otapp.hmis.engine.billing.claim.domain;

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
 * A per-payer insurance claim: the COVERED invoice lines routed to one
 * {@code (payerPlan, member)} aggregated into a single submittable document.
 *
 * <p>The claim is an insurer-side accounts-receivable record. It is built from
 * lines the insurer already "paid" at charge time (covered bills land at patient
 * balance 0 via {@code Invoice.totalCovered}), so its lifecycle and
 * {@code settledAmount} are decoupled from the patient ledger — submitting,
 * settling or rejecting a claim NEVER mutates an invoice.
 */
@Entity
@Table(name = "insurance_claim",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_insurance_claim_no", columnNames = "claim_no")
       },
       indexes = {
               @Index(name = "idx_insurance_claim_plan",     columnList = "payer_plan_uid"),
               @Index(name = "idx_insurance_claim_provider", columnList = "provider_uid"),
               @Index(name = "idx_insurance_claim_status",   columnList = "status"),
               @Index(name = "idx_insurance_claim_member",   columnList = "membership_no")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_no", nullable = false, length = 32)
    private String claimNo;

    /** The plan the covered lines were routed to (grouping key + payer identity). */
    @Column(name = "payer_plan_uid", nullable = false, length = 26) private String payerPlanUid;
    /** The insurer the claim is addressed to (snapshot of the plan's provider at build). */
    @Column(name = "provider_uid",   nullable = false, length = 26) private String providerUid;
    /** Member identity (second half of the grouping key). */
    @Column(name = "membership_no",  nullable = false, length = 64) private String membershipNo;
    /** The insured patient (one member = one patient here; carried for cross-check). */
    @Column(name = "patient_uid",    nullable = false, length = 26) private String patientUid;

    @Column(nullable = false, length = 3) private String currency;
    /** Sum of claim-line amounts at build; immutable after build. */
    @Column(name = "claimed_amount", nullable = false, precision = 14, scale = 2) private BigDecimal claimedAmount;
    /** Running insurer-paid total (supports partial settlement). */
    @Column(name = "settled_amount", nullable = false, precision = 14, scale = 2) private BigDecimal settledAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Column(name = "line_count", nullable = false) private int lineCount;

    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "settled_at")   private Instant settledAt;
    @Column(name = "rejected_at")  private Instant rejectedAt;
    @Column(name = "rejection_reason", length = 500) private String rejectionReason;
    @Column(name = "submitted_by_username", length = 64) private String submittedByUsername;
    @Column(name = "settled_by_username",   length = 64) private String settledByUsername;
    @Column(name = "rejected_by_username",  length = 64) private String rejectedByUsername;

    @SuppressWarnings("java:S107") // built once by the service from gathered lines
    public Claim(String claimNo, String payerPlanUid, String providerUid, String membershipNo,
                 String patientUid, String currency, BigDecimal claimedAmount, int lineCount) {
        if (claimedAmount == null || claimedAmount.signum() < 0) {
            throw new BusinessRuleException("Claimed amount must be non-negative");
        }
        if (lineCount <= 0) {
            throw new BusinessRuleException("A claim must have at least one line");
        }
        this.claimNo = claimNo;
        this.payerPlanUid = payerPlanUid;
        this.providerUid = providerUid;
        this.membershipNo = membershipNo;
        this.patientUid = patientUid;
        this.currency = (currency == null || currency.isBlank()) ? "TZS" : currency;
        this.claimedAmount = claimedAmount;
        this.lineCount = lineCount;
    }

    /** Outstanding insurer balance — what the payer still owes on this claim. */
    public BigDecimal outstanding() {
        return claimedAmount.subtract(settledAmount);
    }

    /** Send the claim to the insurer. DRAFT → SUBMITTED. */
    public void submit(String username) {
        if (status != ClaimStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT claim can be submitted (current: " + status + ")");
        }
        status = ClaimStatus.SUBMITTED;
        submittedAt = Instant.now();
        submittedByUsername = username;
    }

    /**
     * Record an insurer payment against a submitted claim. Advances to
     * PARTIALLY_SETTLED, or SETTLED once the full claimed amount is covered.
     * Does NOT touch any invoice — the patient balance was already settled at
     * charge time.
     */
    public void recordSettlement(BigDecimal amount, String username) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Settlement amount must be positive");
        }
        if (status != ClaimStatus.SUBMITTED && status != ClaimStatus.PARTIALLY_SETTLED) {
            throw new BusinessRuleException(
                    "Only a SUBMITTED or PARTIALLY_SETTLED claim can be settled (current: " + status + ")");
        }
        BigDecimal newSettled = settledAmount.add(amount);
        if (newSettled.compareTo(claimedAmount) > 0) {
            throw new BusinessRuleException("Settlement exceeds the claimed amount");
        }
        settledAmount = newSettled;
        settledByUsername = username;
        if (settledAmount.compareTo(claimedAmount) >= 0) {
            status = ClaimStatus.SETTLED;
            settledAt = Instant.now();
        } else {
            status = ClaimStatus.PARTIALLY_SETTLED;
        }
    }

    /** Insurer declined the claim. SUBMITTED/PARTIALLY_SETTLED → REJECTED; SETTLED is terminal. */
    public void reject(String reason, String username) {
        if (status != ClaimStatus.SUBMITTED && status != ClaimStatus.PARTIALLY_SETTLED) {
            throw new BusinessRuleException(
                    "Only a SUBMITTED or PARTIALLY_SETTLED claim can be rejected (current: " + status + ")");
        }
        status = ClaimStatus.REJECTED;
        rejectedAt = Instant.now();
        rejectionReason = reason;
        rejectedByUsername = username;
    }

    /** A DRAFT claim may be discarded (its lines released back to claimable). */
    public boolean isDiscardable() {
        return status == ClaimStatus.DRAFT;
    }
}
