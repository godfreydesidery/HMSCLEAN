package com.otapp.hmis.engine.patient;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end test for the OUTSIDER (walk-in) pathway:
 *
 *   Register patient as OUTSIDER → raise an outsider lab order with no
 *   consultation → mark order COMPLETED → regenerate the outsider invoice
 *   → assert the invoice picks up the line and the {@code scope} field
 *   correctly reports OUTSIDER.
 *
 * Uses the seeded CBC lab test type (V4).
 */
class OutsiderPathwayIT extends AuthenticatedIntegrationTest {

    private static final String CBC_LAB_TEST_UID = "01J5KQRPCD0000000000000LB1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void outsiderOrderFlowsThroughBilling() {
        // 1. Register an OUTSIDER patient.
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Walk",
                        "lastName",    "InWalter",
                        "dateOfBirth", LocalDate.now().minusYears(40).toString(),
                        "gender",      "MALE",
                        "type",        "OUTSIDER",
                        "paymentType", "CASH"),
                Map.class)).get("uid");

        // 2. Raise an outsider lab order (no consultation).
        Map order = expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders",
                Map.of(
                        "kind",       "LAB_TEST",
                        "serviceUid", CBC_LAB_TEST_UID,
                        "urgency",    "NORMAL"),
                Map.class));
        String orderUid = (String) order.get("uid");
        assertThat(order.get("status")).isEqualTo("REQUESTED");
        assertThat(order.get("consultationUid")).isNull();
        assertThat(order.get("patientUid")).isEqualTo(patientUid);

        // 3. Mark it COMPLETED so it becomes billable.
        Map completed = expectOk(post(
                "/encounters/orders/uid/" + orderUid + "/complete",
                Map.of("result", "WBC 5.6, HGB 14.1, PLT 220 — within range"),
                Map.class));
        assertThat(completed.get("status")).isEqualTo("COMPLETED");

        // 4. Generate the OUTSIDER invoice for the patient.
        Map invoice = expectOk(post(
                "/billing/patients/uid/" + patientUid + "/outsider-invoice",
                null,
                Map.class));
        assertThat(invoice.get("scope")).isEqualTo("OUTSIDER");
        assertThat(invoice.get("patientUid")).isEqualTo(patientUid);
        assertThat(invoice.get("consultationUid")).isNull();
        assertThat(invoice.get("admissionUid")).isNull();

        // 5. The lab order should be one of the lines.
        List<Map<String, Object>> lines = (List<Map<String, Object>>) invoice.get("lines");
        assertThat(lines)
                .as("Outsider invoice should pick up the COMPLETED lab order")
                .anySatisfy(line -> {
                    assertThat(line.get("kind")).isEqualTo("LAB_TEST");
                    assertThat(line.get("referenceUid")).isEqualTo(orderUid);
                });
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
