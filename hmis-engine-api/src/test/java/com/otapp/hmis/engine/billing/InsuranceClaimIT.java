package com.otapp.hmis.engine.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end exercise of the insurance-claim ledger
 * ({@link com.otapp.hmis.engine.billing.claim.api.ClaimController} +
 * {@code ClaimService}), driven entirely over REST against the Testcontainers
 * Postgres.
 *
 * <p>The hard part is producing a real COVERED invoice line: per
 * {@code CoverageResolver}, a line is COVERED only when the encounter's plan
 * flags the service covered AND a cash baseline price exists. So each test:
 *
 * <ol>
 *   <li>seeds a cash price and a <em>covered</em> plan price for the lab test
 *       (both equal, so there is no co-pay top-up line — one clean COVERED line);</li>
 *   <li>registers an INSURANCE patient on the NHIF plan with a membership number;</li>
 *   <li>books an INSURANCE consultation (covered → openable immediately), opens it,
 *       raises a lab order, and charges it onto the consultation invoice;</li>
 *   <li>asserts the invoice carries the covered line at patient balance 0
 *       (the covered amount nets out via {@code Invoice.totalCovered});</li>
 *   <li>assembles, submits and settles / rejects the claim.</li>
 * </ol>
 *
 * <p>The claimable-lines query groups by {@code (payerPlanUid, membershipNo)}
 * across every invoice in the shared (JVM-lifetime) container, so each test uses
 * a <b>unique membership number</b> to keep its covered line isolated from any
 * other test's NHIF lines.
 *
 * <p>Uses the seeded OPD clinic (V3), the NHIF plan (V5, provider IN1), and the
 * FBG lab test (V4). The FBG cell is distinct from the CBC cell exercised by
 * {@code PayBeforeServiceIT}, so the price-cell creates do not collide.
 */
class InsuranceClaimIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String NHIF_PLAN_UID  = "01J5KQRPCD0000000000000IP1";
    private static final String FBG_LAB_UID    = "01J5KQRPCD0000000000000LB3";
    /** Covered == cash price, so the covered line carries no co-pay top-up. */
    private static final BigDecimal COVERED_AMOUNT = new BigDecimal("12000.00");

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assemblesSubmitsAndFullySettlesAClaimFromACoveredLine() {
        String membershipNo = uniqueMembershipNo();
        String consultationUid = chargeCoveredLabLineFor(membershipNo);

        // 2. The consultation invoice now carries the covered line. The insurer
        //    pays it up front (Invoice.totalCovered), so the patient balance is 0
        //    even though no cash payment was recorded. We surface that as
        //    subtotal == covered amount, totalPaid == 0, balance == 0.
        Map<String, Object> invoice = expectOk(get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        BigDecimal subtotalBefore = bd(invoice, "subtotal");
        BigDecimal paidBefore     = bd(invoice, "totalPaid");
        BigDecimal balanceBefore  = bd(invoice, "balance");
        assertThat(subtotalBefore)
                .as("Covered lab line lands on the invoice subtotal")
                .isEqualByComparingTo(COVERED_AMOUNT);
        assertThat(paidBefore)
                .as("No cash was taken — the insurer covers it")
                .isEqualByComparingTo("0.00");
        assertThat(balanceBefore)
                .as("Covered portion nets out of the patient balance (totalCovered > 0)")
                .isEqualByComparingTo("0.00");
        // The invoice also carries the (zero-amount, unpriced) consultation-fee line,
        // so filter to the COVERED line the claim will actually gather.
        List<Map<String, Object>> lines = (List<Map<String, Object>>) invoice.get("lines");
        List<Map<String, Object>> covered = lines.stream()
                .filter(l -> "COVERED".equals(l.get("coverageStatus"))).toList();
        assertThat(covered).hasSize(1);
        assertThat(covered.get(0).get("membershipNo")).isEqualTo(membershipNo);
        assertThat(covered.get(0).get("payerPlanUid")).isEqualTo(NHIF_PLAN_UID);

        // 3. Assemble the claim from the unclaimed covered line(s).
        Map<String, Object> claim = expectCreated(post(
                "/billing/claims",
                Map.of("payerPlanUid", NHIF_PLAN_UID, "membershipNo", membershipNo),
                Map.class));
        String claimUid = (String) claim.get("uid");
        assertThat(claim.get("status")).isEqualTo("DRAFT");
        assertThat(claim.get("payerPlanUid")).isEqualTo(NHIF_PLAN_UID);
        assertThat(claim.get("membershipNo")).isEqualTo(membershipNo);
        assertThat(((Number) claim.get("lineCount")).intValue()).isEqualTo(1);
        assertThat(bd(claim, "claimedAmount")).isEqualByComparingTo(COVERED_AMOUNT);
        assertThat(bd(claim, "settledAmount")).isEqualByComparingTo("0.00");
        assertThat(bd(claim, "outstanding")).isEqualByComparingTo(COVERED_AMOUNT);

        // 4. No double-claim: the line is now stamped, so a second assemble for
        //    the same plan + member has no unclaimed covered lines → 4xx.
        ResponseEntity<Map> again = post(
                "/billing/claims",
                Map.of("payerPlanUid", NHIF_PLAN_UID, "membershipNo", membershipNo),
                Map.class);
        assertThat(again.getStatusCode().is4xxClientError())
                .as("Re-assembling with no unclaimed covered lines must be refused (got %s, body %s)",
                        again.getStatusCode(), again.getBody())
                .isTrue();

        // 5. Balance untouched: stamping claim_id on the line must NOT mutate any
        //    invoice money field — the patient ledger is decoupled from the claim.
        Map<String, Object> invoiceAfter = expectOk(get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        assertThat(bd(invoiceAfter, "subtotal")).isEqualByComparingTo(subtotalBefore);
        assertThat(bd(invoiceAfter, "totalPaid")).isEqualByComparingTo(paidBefore);
        assertThat(bd(invoiceAfter, "balance")).isEqualByComparingTo(balanceBefore);

        // 6. Lifecycle: submit → partially settle → settle.
        Map<String, Object> submitted = expectOk(post(
                "/billing/claims/uid/" + claimUid + "/submit", null, Map.class));
        assertThat(submitted.get("status")).isEqualTo("SUBMITTED");
        assertThat(submitted.get("submittedAt")).isNotNull();

        BigDecimal half = COVERED_AMOUNT.divide(new BigDecimal("2"));
        Map<String, Object> partial = expectOk(post(
                "/billing/claims/uid/" + claimUid + "/settlements",
                Map.of("amount", half, "reference", "EFT-1"), Map.class));
        assertThat(partial.get("status")).isEqualTo("PARTIALLY_SETTLED");
        assertThat(bd(partial, "settledAmount")).isEqualByComparingTo(half);
        assertThat(bd(partial, "outstanding")).isEqualByComparingTo(COVERED_AMOUNT.subtract(half));

        BigDecimal rest = COVERED_AMOUNT.subtract(half);
        Map<String, Object> settled = expectOk(post(
                "/billing/claims/uid/" + claimUid + "/settlements",
                Map.of("amount", rest, "reference", "EFT-2"), Map.class));
        assertThat(settled.get("status")).isEqualTo("SETTLED");
        assertThat(bd(settled, "settledAmount")).isEqualByComparingTo(COVERED_AMOUNT);
        assertThat(bd(settled, "outstanding")).isEqualByComparingTo("0.00");
        assertThat(settled.get("settledAt")).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void submittedClaimCanBeRejected() {
        String membershipNo = uniqueMembershipNo();
        chargeCoveredLabLineFor(membershipNo);

        Map<String, Object> claim = expectCreated(post(
                "/billing/claims",
                Map.of("payerPlanUid", NHIF_PLAN_UID, "membershipNo", membershipNo),
                Map.class));
        String claimUid = (String) claim.get("uid");

        expectOk(post("/billing/claims/uid/" + claimUid + "/submit", null, Map.class));

        Map<String, Object> rejected = expectOk(post(
                "/billing/claims/uid/" + claimUid + "/reject",
                Map.of("reason", "Member not eligible on service date"), Map.class));
        assertThat(rejected.get("status")).isEqualTo("REJECTED");
        assertThat(rejected.get("rejectionReason")).isEqualTo("Member not eligible on service date");
        assertThat(rejected.get("rejectedAt")).isNotNull();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void claimEndpointRequiresBillingAccess() {
        // A user with no BILLING_ACCESS privilege (HR holds only HR_ACCESS) is
        // refused by the controller-level @PreAuthorize.
        String hrToken = tokenForRoles("hr", Set.of("HR"));
        ResponseEntity<Map> forbidden = getAs(hrToken, "/billing/claims", Map.class);
        assertThat(forbidden.getStatusCode().value())
                .as("Claim search without BILLING_ACCESS must be 403 (got %s)", forbidden.getStatusCode())
                .isEqualTo(403);
    }

    // ----- flow helpers ------------------------------------------------------

    /**
     * Drives the full charge flow for one (plan, member) and returns the
     * consultation uid: ensure prices → register INSURANCE patient → book + open
     * an INSURANCE consultation → raise a lab order → charge it. Leaves exactly
     * one COVERED line on the consultation invoice for {@code membershipNo}.
     */
    private String chargeCoveredLabLineFor(String membershipNo) {
        ensureCashPrice();
        ensureCoveredPlanPrice();

        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",        "Covered",
                        "lastName",         "Claimant",
                        "dateOfBirth",      LocalDate.now().minusYears(40).toString(),
                        "gender",           "FEMALE",
                        "type",             "OUTPATIENT",
                        "paymentType",      "INSURANCE",
                        "insurancePlanUid", NHIF_PLAN_UID,
                        "membershipNo",     membershipNo),
                Map.class)).get("uid");

        // INSURANCE consultation is covered → queued + openable with no fee payment.
        String consultationUid = (String) expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",        patientUid,
                        "clinicUid",         OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType",       "INSURANCE",
                        "insurancePlanUid",  NHIF_PLAN_UID,
                        "reason",            "fever"),
                Map.class)).get("uid");

        openConsultation(consultationUid);

        // Doctor raises a lab order; charge it onto the consultation invoice. The
        // raise listener bills it after-commit, but the explicit /charge call is
        // idempotent and makes the bill deterministic within the test.
        String orderUid = (String) expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/orders",
                Map.of("kind", "LAB_TEST", "serviceUid", FBG_LAB_UID, "urgency", "NORMAL"),
                Map.class)).get("uid");

        expectOk(post("/billing/orders/uid/" + orderUid + "/charge", null, Void.class));
        return consultationUid;
    }

    /** Cash baseline price for FBG so {@code PriceLookup} resolves a non-zero amount. */
    @SuppressWarnings("rawtypes")
    private void ensureCashPrice() {
        ResponseEntity<Map> r = post(
                "/masterdata/service-prices",
                Map.of("kind", "LAB_TEST", "serviceUid", FBG_LAB_UID,
                        "amount", COVERED_AMOUNT, "currency", "TZS", "covered", false),
                Map.class);
        assertOkOrConflict(r);
    }

    /** Covered plan price for FBG on NHIF (covered == cash → no co-pay top-up line). */
    @SuppressWarnings("rawtypes")
    private void ensureCoveredPlanPrice() {
        ResponseEntity<Map> r = post(
                "/masterdata/service-prices",
                Map.of("kind", "LAB_TEST", "serviceUid", FBG_LAB_UID,
                        "amount", COVERED_AMOUNT, "currency", "TZS",
                        "planUid", NHIF_PLAN_UID, "covered", true),
                Map.class);
        assertOkOrConflict(r);
    }

    // ----- assertion helpers (mirror CashierShiftIT / RegistrationFeeIT) ------

    private static String uniqueMembershipNo() {
        return "CLM-" + Long.toString(System.nanoTime(), 36);
    }

    private static BigDecimal bd(Map<String, Object> body, String field) {
        Object v = body.get(field);
        assertThat(v).as("field '%s' present", field).isNotNull();
        return new BigDecimal(v.toString());
    }

    /** Price-cell create is create-only: 2xx fresh, or 409 if a prior run made the cell. */
    private static void assertOkOrConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 409)
                .as("Expected 2xx or 409 but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        return response.getBody();
    }

    private static <T> T expectCreated(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().value())
                .as("Expected 201 but got %s with body %s", response.getStatusCode(), response.getBody())
                .isEqualTo(201);
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
