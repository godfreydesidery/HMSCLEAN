package com.otapp.hmis.engine.patient;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

/**
 * Registration cluster (gap audit cluster 7):
 *   * REG-1 — a patient can be found by insurance membership / card number, not
 *     just name / file-no / phone / national-id.
 *   * REG-2 — converting a walk-in (OUTSIDER) into a registered patient sweeps
 *     the now-orphaned walk-in work: open outsider orders + prescriptions are
 *     cancelled and the draft outsider invoice is discarded.
 */
class RegistrationSearchSweepIT extends AuthenticatedIntegrationTest {

    private static final String CBC_UID    = "01J5KQRPCD0000000000000LB1";
    private static final String AMOXIL_UID = "01J5KQRPCD0000000000000MD2";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {};

    @Test
    @SuppressWarnings("unchecked")
    void patientIsFoundByMembershipCardNumber() {
        String card = "CARD-" + Long.toString(System.nanoTime(), 36).toUpperCase();
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.ofEntries(
                        Map.entry("firstName", "Card"), Map.entry("lastName", "Holder"),
                        Map.entry("dateOfBirth", LocalDate.now().minusYears(40).toString()),
                        Map.entry("gender", "MALE"), Map.entry("type", "OUTPATIENT"),
                        Map.entry("paymentType", "CASH"), Map.entry("membershipNo", card)),
                Map.class)).get("uid");

        Map<String, Object> page = expectOk(get("/patients?size=50&query=" + card, MAP));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).as("search by membership/card number finds the patient")
                .anyMatch(p -> patientUid.equals(p.get("uid")));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void leavingOutsiderSweepsOpenWorkAndDraftInvoice() {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of("firstName", "Walk", "lastName", "Convert",
                        "dateOfBirth", LocalDate.now().minusYears(28).toString(),
                        "gender", "FEMALE", "type", "OUTSIDER", "paymentType", "CASH"),
                Map.class)).get("uid");

        String orderUid = (String) expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders",
                Map.of("kind", "LAB_TEST", "serviceUid", CBC_UID, "urgency", "NORMAL"),
                Map.class)).get("uid");
        String rxUid = (String) expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-prescriptions",
                Map.of("medicineUid", AMOXIL_UID, "dose", "1 tab", "frequency", "OD", "quantity", 5),
                Map.class)).get("uid");
        // An outsider invoice, then ISSUED-but-unpaid (the harder orphan: it must
        // still be swept even though it is no longer DRAFT).
        String invoiceUid = (String) expectOk(post(
                "/billing/patients/uid/" + patientUid + "/outsider-invoice", null, Map.class)).get("uid");
        expectOk(post("/billing/invoices/uid/" + invoiceUid + "/issue", null, Map.class));
        assertThat(expectOk(get("/billing/invoices/uid/" + invoiceUid, Map.class)).get("status"))
                .isEqualTo("ISSUED");

        // Convert the walk-in to a registered outpatient → sweep fires (after-commit).
        expectOk(put("/patients/uid/" + patientUid + "/type", Map.of("type", "OUTPATIENT"), Map.class));

        assertThat(outsiderOrderStatus(patientUid, orderUid))
                .as("open outsider order is cancelled on conversion").isEqualTo("CANCELLED");
        assertThat(outsiderRxStatus(patientUid, rxUid))
                .as("open outsider prescription is cancelled on conversion").isEqualTo("CANCELLED");
        assertThat(expectOk(get("/billing/invoices/uid/" + invoiceUid, Map.class)).get("status"))
                .as("unpaid (issued) outsider invoice is discarded on conversion").isEqualTo("CANCELLED");
    }

    // ----- helpers ---------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String outsiderOrderStatus(String patientUid, String orderUid) {
        List<Map<String, Object>> orders = expectOk(get(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders", List.class));
        return (String) orders.stream().filter(o -> orderUid.equals(o.get("uid")))
                .findFirst().map(o -> o.get("status")).orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String outsiderRxStatus(String patientUid, String rxUid) {
        List<Map<String, Object>> scripts = expectOk(get(
                "/encounters/patients/uid/" + patientUid + "/outsider-prescriptions", List.class));
        return (String) scripts.stream().filter(s -> rxUid.equals(s.get("uid")))
                .findFirst().map(s -> s.get("status")).orElse(null);
    }

    private static <T> T expectOk(org.springframework.http.ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
