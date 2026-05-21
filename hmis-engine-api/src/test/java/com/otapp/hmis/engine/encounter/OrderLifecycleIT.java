package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The restored clinical-order gates (PROCESS_MISMATCHES.md M14/M16):
 *   * a PROCEDURE must be APPROVED (surgeon sign-off) before it can be worked;
 *   * a LAB_TEST must be ACCEPTED (specimen collected) before it can be worked;
 *   * an order cannot jump REQUESTED → COMPLETED (must pass through IN_PROGRESS).
 *
 * Uses the seeded Wound-Dressing procedure (PC1) + CBC lab (LB1), raised on an
 * OUTSIDER so no consultation setup is needed.
 */
class OrderLifecycleIT extends AuthenticatedIntegrationTest {

    private static final String PROCEDURE_UID = "01J5KQRPCD0000000000000PC1";
    private static final String CBC_UID       = "01J5KQRPCD0000000000000LB1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void procedureRequiresApprovalBeforeWork() {
        String patientUid = registerOutsider("Proc", "Patient");
        String orderUid = raiseOutsiderOrder(patientUid, "PROCEDURE", PROCEDURE_UID);

        // Cannot complete or start a REQUESTED procedure; accept() is for lab/radiology.
        assertThat(post("/encounters/orders/uid/" + orderUid + "/complete", null, Map.class)
                .getStatusCode().is4xxClientError()).isTrue();
        assertThat(post("/encounters/orders/uid/" + orderUid + "/start", null, Map.class)
                .getStatusCode().is4xxClientError()).isTrue();
        assertThat(post("/encounters/orders/uid/" + orderUid + "/accept", null, Map.class)
                .getStatusCode().is4xxClientError()).isTrue();

        // Approve (sign-off) → start → complete.
        assertThat(expectOk(post("/encounters/orders/uid/" + orderUid + "/approve", null, Map.class))
                .get("status")).isEqualTo("APPROVED");
        assertThat(expectOk(post("/encounters/orders/uid/" + orderUid + "/start", null, Map.class))
                .get("status")).isEqualTo("IN_PROGRESS");
        assertThat(expectOk(post("/encounters/orders/uid/" + orderUid + "/complete",
                Map.of("result", "uneventful"), Map.class)).get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void labRequiresAcceptanceBeforeWork() {
        String patientUid = registerOutsider("Lab", "Patient");
        String orderUid = raiseOutsiderOrder(patientUid, "LAB_TEST", CBC_UID);

        // approve() is procedure-only; start() needs ACCEPTED first; no REQUESTED → COMPLETED jump.
        assertThat(post("/encounters/orders/uid/" + orderUid + "/approve", null, Map.class)
                .getStatusCode().is4xxClientError()).isTrue();
        assertThat(post("/encounters/orders/uid/" + orderUid + "/start", null, Map.class)
                .getStatusCode().is4xxClientError()).isTrue();
        assertThat(post("/encounters/orders/uid/" + orderUid + "/complete", null, Map.class)
                .getStatusCode().is4xxClientError()).isTrue();

        assertThat(expectOk(post("/encounters/orders/uid/" + orderUid + "/accept", null, Map.class))
                .get("status")).isEqualTo("ACCEPTED");
        assertThat(expectOk(post("/encounters/orders/uid/" + orderUid + "/start", null, Map.class))
                .get("status")).isEqualTo("IN_PROGRESS");
        assertThat(expectOk(post("/encounters/orders/uid/" + orderUid + "/complete",
                Map.of("result", "within range"), Map.class)).get("status")).isEqualTo("COMPLETED");
    }

    private String registerOutsider(String first, String last) {
        return stringField(expectOk(post(
                "/patients",
                Map.of("firstName", first, "lastName", last,
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender", "MALE", "type", "OUTSIDER", "paymentType", "CASH"),
                Map.class)), "uid");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String raiseOutsiderOrder(String patientUid, String kind, String serviceUid) {
        Map order = expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders",
                Map.of("kind", kind, "serviceUid", serviceUid, "urgency", "NORMAL"),
                Map.class));
        assertThat(order.get("status")).isEqualTo("REQUESTED");
        return (String) order.get("uid");
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
