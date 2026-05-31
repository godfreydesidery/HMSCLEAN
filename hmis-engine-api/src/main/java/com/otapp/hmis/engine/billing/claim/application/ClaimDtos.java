package com.otapp.hmis.engine.billing.claim.application;

import com.otapp.hmis.engine.billing.claim.domain.ClaimStatus;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ClaimDtos {

    private ClaimDtos() {}

    public record ClaimDto(
            Long id,
            String uid,
            String claimNo,
            String payerPlanUid,
            String payerPlanName,
            String providerUid,
            String providerName,
            String membershipNo,
            String patientUid,
            String currency,
            BigDecimal claimedAmount,
            BigDecimal settledAmount,
            BigDecimal outstanding,
            ClaimStatus status,
            int lineCount,
            Instant submittedAt,
            Instant settledAt,
            Instant rejectedAt,
            String rejectionReason,
            String submittedByUsername,
            String settledByUsername,
            String rejectedByUsername,
            Instant createdAt,
            List<ClaimLineDto> lines) {}

    public record ClaimSummary(
            Long id,
            String uid,
            String claimNo,
            String payerPlanUid,
            String payerPlanName,
            String providerName,
            String membershipNo,
            String patientUid,
            String currency,
            BigDecimal claimedAmount,
            BigDecimal settledAmount,
            ClaimStatus status,
            int lineCount,
            Instant createdAt) {}

    public record ClaimLineDto(
            Long id,
            String uid,
            Long invoiceLineId,
            String serviceUid,
            InvoiceLineKind kind,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount) {}

    /** Build a DRAFT claim from the unclaimed COVERED lines for a (payer plan, member). */
    public record AssembleClaimRequest(
            @NotBlank @Size(min = 26, max = 26) String payerPlanUid,
            @NotBlank @Size(max = 64) String membershipNo) {}

    public record RecordSettlementRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @Size(max = 120) String reference,
            @Size(max = 500) String note) {}

    public record RejectClaimRequest(@NotBlank @Size(max = 500) String reason) {}
}
