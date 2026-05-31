package com.otapp.hmis.engine.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Admission-billing fidelity — process-audit cluster #1 + #3
 * (PROCESS_MISMATCHES.md M23, reopened).
 *
 * <p>Admitting a patient must seed <em>and ISSUE</em> the ward-bed invoice via the
 * {@code AdmissionAdmittedEvent} → {@code AdmissionFeeListeners} after-commit path,
 * so a bill exists and the discharge bill-clearance gate is armed the moment the
 * patient is admitted (a patient can no longer be discharged having paid nothing).
 * The invoice keeps accruing ward-days while it is unpaid (the nightly
 * {@code AdmissionAccrualJob} rebuilds it through the same {@code generateForAdmission}
 * path), and freezes once a payment is taken.
 *
 * <p>Mirrors {@link RegistrationFeeIT}'s after-commit fee-seeding pattern. Uses the
 * V4-seeded General Ward and seeds a cash WARD price so {@code PriceLookup} resolves
 * a non-zero ward-day amount.
 */
class AdmissionBillingIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID   = "01J5KQRPCD0000000000000CN1";
    // Pediatric ward — priced here (and not on the General Ward) so the deposit-gate
    // pricing does not leak onto the General Ward used by the encounter charting ITs.
    private static final String PEDIATRIC_WARD_UID = "01J5KQRPCD0000000000000WG2";
    private static final BigDecimal WARD_DAY     = new BigDecimal("20000.00");

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void admittingIssuesTheWardBedInvoiceArmsTheGateAndAccruesUntilPaid() {
        ensureWardPrice();
        String patientUid = registerCashPatient();

        // 1. Admit — the after-commit listener seeds + issues the ward-bed invoice.
        Map<String, Object> admission = expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", patientUid,
                        "wardUid", PEDIATRIC_WARD_UID,
                        "admittingClinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH",
                        "admissionReason", "observation"),
                Map.class));
        String admissionUid = (String) admission.get("uid");
        // CASH admit to a priced ward holds the admission deposit-pending until the
        // ward-bed bill is settled (the deposit gate, cluster #2).
        assertThat(admission.get("status")).isEqualTo("AWAITING_DEPOSIT");

        // 2. The after-commit listener seeded + ISSUED the ward-bed invoice with
        //    one ward-day at the seeded price. (The admit *response* is built before
        //    the after-commit listener runs, so the gate state is read fresh below.)
        Map<String, Object> invoice = expectOk(get(
                "/billing/admissions/uid/" + admissionUid + "/invoice", Map.class));
        assertThat(invoice.get("scope")).isEqualTo("ADMISSION");
        assertThat(invoice.get("status")).isEqualTo("ISSUED");
        assertThat(new BigDecimal(invoice.get("subtotal").toString()))
                .as("One ward-day at the seeded WARD price")
                .isEqualByComparingTo(WARD_DAY);
        String invoiceUid = (String) invoice.get("uid");

        // 3. Gate armed: re-read the admission — an outstanding ward-bed bill now
        //    exists, so bills are NOT cleared and discharge is blocked.
        Map<String, Object> armed = expectOk(get(
                "/encounters/admissions/uid/" + admissionUid, Map.class));
        assertThat(armed.get("billsCleared"))
                .as("Issuing the ward-bed invoice arms the discharge gate")
                .isEqualTo(Boolean.FALSE);

        // 3. Accrual still allowed while ISSUED + unpaid — this is the path the
        //    nightly AdmissionAccrualJob takes; it must not throw.
        Map<String, Object> reaccrued = expectOk(post(
                "/billing/admissions/uid/" + admissionUid + "/invoice", null, Map.class));
        assertThat(reaccrued.get("status")).isEqualTo("ISSUED");
        assertThat(reaccrued.get("uid")).isEqualTo(invoiceUid);

        // 4. Pay it in full → the discharge gate clears.
        expectOk(post("/billing/invoices/uid/" + invoiceUid + "/payments",
                Map.of("method", "CASH", "amount", WARD_DAY, "currency", "TZS"),
                Map.class));
        Map<String, Object> afterPay = expectOk(get(
                "/encounters/admissions/uid/" + admissionUid, Map.class));
        assertThat(afterPay.get("billsCleared"))
                .as("Full payment clears the discharge gate")
                .isEqualTo(Boolean.TRUE);
        assertThat(afterPay.get("status"))
                .as("Settling the deposit activates the admission")
                .isEqualTo("ADMITTED");

        // 5. Once paid, the invoice is frozen — no further accrual rebuild.
        ResponseEntity<Map> frozen = post(
                "/billing/admissions/uid/" + admissionUid + "/invoice", null, Map.class);
        assertThat(frozen.getStatusCode().is4xxClientError())
                .as("A paid admission invoice must not be regenerable, got %s", frozen.getStatusCode())
                .isTrue();
    }

    // ----- fixtures ----------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private String registerCashPatient() {
        ResponseEntity<Map> r = post(
                "/patients",
                Map.of("firstName", "Ward",
                        "lastName", "Stayer",
                        "dateOfBirth", LocalDate.now().minusYears(40).toString(),
                        "gender", "MALE",
                        "type", "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class);
        return (String) expectOk(r).get("uid");
    }

    /** Cash ward-day price so {@code PriceLookup} resolves a non-zero amount. */
    @SuppressWarnings("rawtypes")
    private void ensureWardPrice() {
        ResponseEntity<Map> r = post(
                "/masterdata/service-prices",
                Map.of("kind", "WARD", "serviceUid", PEDIATRIC_WARD_UID,
                        "amount", WARD_DAY, "currency", "TZS", "covered", false),
                Map.class);
        assertThat(r.getStatusCode().is2xxSuccessful() || r.getStatusCode().value() == 409)
                .as("WARD price seed expected 2xx or 409 (already seeded), got %s", r.getStatusCode())
                .isTrue();
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).as("2xx response had null body").isNotNull();
        return body;
    }
}
