package com.otapp.hmis.engine.billing.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.otapp.hmis.engine.billing.claim.domain.Claim;
import com.otapp.hmis.engine.billing.claim.domain.ClaimStatus;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLine;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceLineKind;
import com.otapp.hmis.engine.billing.invoice.domain.LineCoverageStatus;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage of the claim lifecycle state machine and the once-only
 * claim stamp on an invoice line (no Spring, no DB).
 */
class ClaimTest {

    private static Claim draftClaim(String claimedAmount, int lineCount) {
        return new Claim("CLM-2026-000001", "01PLAN0000000000000000000A", "01PROV0000000000000000000A",
                "MEM-1", "01PT000000000000000000000A", "TZS", new BigDecimal(claimedAmount), lineCount);
    }

    private static InvoiceLine coveredLine() {
        return InvoiceLine.routed("01INV0000000000000000000A", InvoiceLineKind.LAB_TEST, "01SVC0000000000000000000A",
                "01ORD0000000000000000000A", "CBC", BigDecimal.ONE, new BigDecimal("80.00"), new BigDecimal("80.00"),
                LineCoverageStatus.COVERED, "MEM-1", "01PLAN0000000000000000000A");
    }

    @Test
    void buildGuardsRejectEmptyOrNegative() {
        assertThatThrownBy(() -> draftClaim("100.00", 0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> draftClaim("-1.00", 1)).isInstanceOf(BusinessRuleException.class);
        Claim c = draftClaim("100.00", 1);
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.DRAFT);
        assertThat(c.outstanding()).isEqualByComparingTo("100.00");
    }

    @Test
    void submitMovesDraftToSubmittedOnce() {
        Claim c = draftClaim("100.00", 1);
        c.submit("clerk");
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(c.getSubmittedAt()).isNotNull();
        assertThat(c.getSubmittedByUsername()).isEqualTo("clerk");
        assertThatThrownBy(() -> c.submit("clerk")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void settlementGoesPartialThenFull() {
        Claim c = draftClaim("100.00", 1);
        c.submit("clerk");
        c.recordSettlement(new BigDecimal("40.00"), "cash");
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.PARTIALLY_SETTLED);
        assertThat(c.getSettledAmount()).isEqualByComparingTo("40.00");
        assertThat(c.outstanding()).isEqualByComparingTo("60.00");
        c.recordSettlement(new BigDecimal("60.00"), "cash");
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(c.getSettledAt()).isNotNull();
        assertThat(c.outstanding()).isEqualByComparingTo("0");
    }

    @Test
    void overSettlementIsRejected() {
        Claim c = draftClaim("100.00", 1);
        c.submit("clerk");
        assertThatThrownBy(() -> c.recordSettlement(new BigDecimal("100.01"), "cash"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void settlementOnDraftIsRejected() {
        Claim c = draftClaim("100.00", 1);
        assertThatThrownBy(() -> c.recordSettlement(new BigDecimal("10.00"), "cash"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectFromSubmittedButNotFromSettled() {
        Claim c = draftClaim("100.00", 1);
        c.submit("clerk");
        c.reject("Member not active", "clerk");
        assertThat(c.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(c.getRejectionReason()).isEqualTo("Member not active");

        Claim settled = draftClaim("50.00", 1);
        settled.submit("clerk");
        settled.recordSettlement(new BigDecimal("50.00"), "cash");
        assertThatThrownBy(() -> settled.reject("too late", "clerk")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markClaimedIsOnceOnlyAndCoveredOnly() {
        InvoiceLine covered = coveredLine();
        covered.markClaimed(7L);
        assertThat(covered.getClaimId()).isEqualTo(7L);
        assertThatThrownBy(() -> covered.markClaimed(8L)).isInstanceOf(BusinessRuleException.class);
        covered.releaseClaim();
        assertThat(covered.getClaimId()).isNull();

        // A cash (UNPAID) line can never be claimed.
        InvoiceLine cash = new InvoiceLine("01INV0000000000000000000A", InvoiceLineKind.LAB_TEST,
                "01SVC0000000000000000000A", null, "CBC", BigDecimal.ONE, new BigDecimal("80.00"), new BigDecimal("80.00"));
        assertThatThrownBy(() -> cash.markClaimed(1L)).isInstanceOf(BusinessRuleException.class);
    }
}
