package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Pay-before-service queue visibility (gap audit cluster 6, DIAG-2 / PHARM-2).
 * The order worklist now defaults to hiding unpaid items — but class-aware:
 *
 *   * an AMBULATORY (outpatient / outsider) order is hidden until its bill is
 *     settled (PAID / COVERED), then it appears;
 *   * an INPATIENT order stays visible even while unpaid — inpatient care runs
 *     on the deposit/credit and the bill clears at discharge.
 *
 * {@code hideUnpaid=false} restores the show-everything view.
 */
class PayBeforeServiceVisibilityIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID   = "01J5KQRPCD0000000000000CN1";
    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";
    private static final String CBC_UID          = "01J5KQRPCD0000000000000LB1";
    private static final BigDecimal CBC_PRICE    = new BigDecimal("8000.00");

    @Test
    void outpatientUnpaidOrderIsHiddenByDefaultAndRevealedWhenSettled() {
        priceService(CBC_UID, CBC_PRICE);
        String patientUid = registerPatient("PayGate", "Outpatient", "OUTPATIENT");
        String consultationUid = bookAndOpen(patientUid);
        String orderUid = raiseOrder(consultationUid, CBC_UID);

        // Unpaid: hidden from the default queue, but visible with hideUnpaid=false.
        assertThat(orderRow(orderUid, true)).as("unpaid outpatient order is hidden by default").isNull();
        Map<String, Object> shown = orderRow(orderUid, false);
        assertThat(shown).as("hideUnpaid=false still shows it").isNotNull();
        assertThat(shown.get("settled")).isEqualTo(false);

        // Settle the order's line → it appears on the default queue.
        payLine(patientUid, lineForOrder(consultationUid, orderUid));
        assertThat(orderRow(orderUid, true)).as("settled order appears on the default queue").isNotNull();
    }

    @Test
    void inpatientUnpaidOrderStaysVisibleByDefault() {
        priceService(CBC_UID, CBC_PRICE);
        String patientUid = registerPatient("PayGate", "Inpatient", "OUTPATIENT");
        String consultationUid = bookAndOpen(patientUid);
        // Admit the patient — now an active admission exists, so a consultation
        // order for them is INPATIENT. (Deposit-pending AWAITING_DEPOSIT counts.)
        expectOk(post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "observation"),
                Map.class));
        String orderUid = raiseOrder(consultationUid, CBC_UID);

        // Unpaid, but inpatient → visible on the default (hideUnpaid=true) queue.
        Map<String, Object> row = orderRow(orderUid, true);
        assertThat(row).as("unpaid inpatient order stays visible by default").isNotNull();
        assertThat(row.get("settled")).as("it really is unpaid — the inpatient carve-out, not a settled order")
                .isEqualTo(false);
    }

    // ----- helpers ---------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private void priceService(String serviceUid, BigDecimal amount) {
        ResponseEntity<Map> resp = post("/masterdata/service-prices",
                Map.of("kind", "LAB_TEST", "serviceUid", serviceUid, "amount", amount, "currency", "TZS"),
                Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode().value() == 409)
                .as("price %s should be set or already exist", serviceUid).isTrue();
    }

    private String registerPatient(String first, String last, String type) {
        return stringField(expectOk(post(
                "/patients",
                Map.of("firstName", first, "lastName", last,
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender", "FEMALE", "type", type, "paymentType", "CASH"),
                Map.class)), "uid");
    }

    private String bookAndOpen(String patientUid) {
        String consultationUid = stringField(expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "fever"),
                Map.class)), "uid");
        openConsultation(consultationUid);
        return consultationUid;
    }

    @SuppressWarnings("rawtypes")
    private String raiseOrder(String consultationUid, String serviceUid) {
        return stringField(expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/orders",
                Map.of("kind", "LAB_TEST", "serviceUid", serviceUid, "urgency", "NORMAL"),
                Map.class)), "uid");
    }

    /** The worklist row for the order at the given pay-gate setting, or null if hidden. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> orderRow(String orderUid, boolean hideUnpaid) {
        Map page = expectOk(get(
                "/encounters/orders?size=200&kind=LAB_TEST&hideUnpaid=" + hideUnpaid, Map.class));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        return content == null ? null
                : content.stream().filter(r -> orderUid.equals(r.get("uid"))).findFirst().orElse(null);
    }

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
    private void payLine(String patientUid, String lineUid) {
        expectOk(post("/billing/patients/uid/" + patientUid + "/pay-lines",
                Map.of("method", "CASH", "currency", "TZS", "lineUids", List.of(lineUid)),
                Map.class));
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
