package com.otapp.hmis.engine.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Line-level cashier payment (legacy {@code confirm_bills_payment} over ticked
 * {@code PatientBill}s). A patient's outstanding service lines are surfaced as a
 * "check to pay" queue ({@code GET .../payable-lines}, segmentable per service
 * till); the cashier pays a selected subset ({@code POST .../pay-lines}); each
 * paid line releases its own order immediately while the rest of the invoice
 * stays unpaid — the core gap over the prior whole-invoice-only settlement.
 *
 * Two cash lab orders are raised onto one consultation invoice. Paying ONLY the
 * CBC line must let the CBC result release while the FBG order is still blocked;
 * paying the FBG line then releases it too.
 */
class CashierLinePaymentIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String CBC_UID        = "01J5KQRPCD0000000000000LB1";
    private static final String FBG_UID        = "01J5KQRPCD0000000000000LB3";
    private static final BigDecimal CBC_PRICE  = new BigDecimal("8000.00");
    private static final BigDecimal FBG_PRICE  = new BigDecimal("6500.00");

    @Test
    void payingOneLineReleasesItsOrderWhileOthersStayGated() {
        priceService(CBC_UID, CBC_PRICE);
        priceService(FBG_UID, FBG_PRICE);

        String patientUid = stringField(expectOk(post(
                "/patients",
                Map.of("firstName", "Line", "lastName", "Cashier",
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender", "FEMALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)), "uid");

        String consultationUid = stringField(expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "fever"),
                Map.class)), "uid");
        openConsultation(consultationUid);

        // Doctor raises two lab orders → two UNPAID cash lines on the invoice.
        String cbcOrder = raiseOrder(consultationUid, CBC_UID);
        String fbgOrder = raiseOrder(consultationUid, FBG_UID);
        // Resolve each order's invoice line by referenceUid (not by amount — the
        // seeded registration fee can share a price with a lab test).
        String cbcLine = lineForOrder(consultationUid, cbcOrder);
        String fbgLine = lineForOrder(consultationUid, fbgOrder);

        // ----- the cashier queue: the lab till lists both lab lines ------------
        List<Map<String, Object>> labTill = payableLines(patientUid, "LAB_TEST");
        assertThat(labTill).extracting(l -> l.get("lineUid")).contains(cbcLine, fbgLine);
        assertThat(payableLines(patientUid, "RADIOLOGY"))
                .as("no radiology lines for this patient").isEmpty();

        // Tech works both orders up to (but not through) completion.
        acceptAndStart(cbcOrder);
        acceptAndStart(fbgOrder);

        // ----- pay ONLY the CBC line ------------------------------------------
        Map cbcPay = expectOk(post(
                "/billing/patients/uid/" + patientUid + "/pay-lines",
                Map.of("method", "CASH", "currency", "TZS", "lineUids", List.of(cbcLine)),
                Map.class));
        assertThat(new BigDecimal(cbcPay.get("totalCollected").toString())).isEqualByComparingTo(CBC_PRICE);
        assertThat(cbcPay.get("lineCount")).isEqualTo(1);

        // CBC released; FBG still gated.
        assertThat(complete(cbcOrder).getStatusCode().is2xxSuccessful())
                .as("paid CBC line releases its order").isTrue();
        assertThat(complete(fbgOrder).getStatusCode().is4xxClientError())
                .as("unpaid FBG line stays gated").isTrue();

        // Invoice only partially paid; the CBC line dropped off the lab till.
        Map partlyPaid = expectOk(get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        assertThat(partlyPaid).containsEntry("status", "PARTIALLY_PAID");
        assertThat(payableLines(patientUid, "LAB_TEST")).extracting(l -> l.get("lineUid"))
                .as("CBC settled, FBG still owed").containsExactly(fbgLine);

        // ----- pay the remaining FBG line -------------------------------------
        expectOk(post(
                "/billing/patients/uid/" + patientUid + "/pay-lines",
                Map.of("method", "CASH", "currency", "TZS", "lineUids", List.of(fbgLine)),
                Map.class));
        assertThat(complete(fbgOrder).getStatusCode().is2xxSuccessful())
                .as("paid FBG line now releases").isTrue();

        Map settled = expectOk(get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        assertThat(settled).containsEntry("status", "PAID");
        assertThat(payableLines(patientUid, "LAB_TEST"))
                .as("both lab lines settled").isEmpty();
    }

    // ----- helpers -----------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private void priceService(String serviceUid, BigDecimal amount) {
        ResponseEntity<Map> resp = post("/masterdata/service-prices",
                Map.of("kind", "LAB_TEST", "serviceUid", serviceUid, "amount", amount, "currency", "TZS"),
                Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode().value() == 409)
                .as("price %s should be set or already exist", serviceUid).isTrue();
    }

    @SuppressWarnings("rawtypes")
    private String raiseOrder(String consultationUid, String serviceUid) {
        return stringField(expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/orders",
                Map.of("kind", "LAB_TEST", "serviceUid", serviceUid, "urgency", "NORMAL"),
                Map.class)), "uid");
    }

    /** The invoice-line uid that bills the given order (matched by referenceUid). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String lineForOrder(String consultationUid, String orderUid) {
        Map invoice = expectOk(get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        List<Map<String, Object>> lines = (List<Map<String, Object>>) invoice.get("lines");
        return lines.stream()
                .filter(l -> orderUid.equals(l.get("referenceUid")))
                .map(l -> (String) l.get("uid"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No invoice line bills order " + orderUid));
    }

    @SuppressWarnings("rawtypes")
    private void acceptAndStart(String orderUid) {
        expectOk(post("/encounters/orders/uid/" + orderUid + "/accept", null, Map.class));
        expectOk(post("/encounters/orders/uid/" + orderUid + "/start", null, Map.class));
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> complete(String orderUid) {
        return post("/encounters/orders/uid/" + orderUid + "/complete",
                Map.of("result", "normal"), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private List<Map<String, Object>> payableLines(String patientUid, String kind) {
        String url = "/billing/patients/uid/" + patientUid + "/payable-lines"
                + (kind == null ? "" : "?kind=" + kind);
        return expectOk(get(url, List.class));
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
