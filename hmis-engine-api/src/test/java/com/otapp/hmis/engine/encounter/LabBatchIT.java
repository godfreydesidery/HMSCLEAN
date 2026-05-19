package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Phase 45 — lab batch processing:
 *
 *   Register 3 OUTSIDER patients → raise 3 CBC orders → create a
 *   single LabBatch grouping all three → walk OPEN → PROCESSING →
 *   COMPLETED. Verify a wrong-test order is refused, and an
 *   already-batched order can't be added to a second batch.
 *
 * Uses CBC (LB1) + FBG (LB3) as the same-test / different-test pair.
 */
class LabBatchIT extends AuthenticatedIntegrationTest {

    private static final String CBC_UID = "01J5KQRPCD0000000000000LB1";
    private static final String FBG_UID = "01J5KQRPCD0000000000000LB3";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void groupCbcOrdersAndWalkLifecycle() {
        String o1 = createOutsiderLabOrder(CBC_UID);
        String o2 = createOutsiderLabOrder(CBC_UID);
        String o3 = createOutsiderLabOrder(CBC_UID);
        String otherTest = createOutsiderLabOrder(FBG_UID);

        // 1. Create the batch with the three CBCs.
        Map batch = expectOk(post(
                "/encounters/lab-batches",
                Map.of(
                        "labTestTypeUid", CBC_UID,
                        "note",           "Morning bench run",
                        "orderUids",      List.of(o1, o2, o3)),
                Map.class));
        String batchUid = (String) batch.get("uid");
        assertThat(batch.get("status")).isEqualTo("OPEN");
        assertThat(((Number) batch.get("memberCount")).intValue()).isEqualTo(3);
        List<String> memberUids = (List<String>) batch.get("memberOrderUids");
        assertThat(memberUids).containsExactlyInAnyOrder(o1, o2, o3);

        // 2. Adding a different-test order is refused.
        ResponseEntity<Map> wrongTest = post(
                "/encounters/lab-batches/uid/" + batchUid + "/orders",
                Map.of("orderUid", otherTest),
                Map.class);
        assertThat(wrongTest.getStatusCode().is4xxClientError())
                .as("FBG order should not fit in a CBC batch")
                .isTrue();

        // 3. Adding an already-batched order to a *new* batch is refused.
        Map secondBatch = expectOk(post(
                "/encounters/lab-batches",
                Map.of(
                        "labTestTypeUid", CBC_UID,
                        "orderUids",      List.of(createOutsiderLabOrder(CBC_UID))),
                Map.class));
        ResponseEntity<Map> dup = post(
                "/encounters/lab-batches/uid/" + secondBatch.get("uid") + "/orders",
                Map.of("orderUid", o1),
                Map.class);
        assertThat(dup.getStatusCode().is4xxClientError())
                .as("An order can sit in at most one batch at a time")
                .isTrue();

        // 4. Lifecycle: OPEN → PROCESSING → COMPLETED.
        Map processing = expectOk(post("/encounters/lab-batches/uid/" + batchUid + "/process", null, Map.class));
        assertThat(processing.get("status")).isEqualTo("PROCESSING");
        assertThat(processing.get("processedAt")).isNotNull();

        Map completed = expectOk(post("/encounters/lab-batches/uid/" + batchUid + "/complete", null, Map.class));
        assertThat(completed.get("status")).isEqualTo("COMPLETED");
        assertThat(completed.get("completedAt")).isNotNull();

        // 5. Adding members to a COMPLETED batch is refused.
        ResponseEntity<Map> postClose = post(
                "/encounters/lab-batches/uid/" + batchUid + "/orders",
                Map.of("orderUid", createOutsiderLabOrder(CBC_UID)),
                Map.class);
        assertThat(postClose.getStatusCode().is4xxClientError()).isTrue();
    }

    private String createOutsiderLabOrder(String labTestUid) {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Lab",
                        "lastName",    "BatchPt-" + System.nanoTime(),
                        "dateOfBirth", LocalDate.now().minusYears(33).toString(),
                        "gender",      "MALE",
                        "type",        "OUTSIDER",
                        "paymentType", "CASH"),
                Map.class)).get("uid");
        return (String) expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders",
                Map.of("kind", "LAB_TEST", "serviceUid", labTestUid, "urgency", "NORMAL"),
                Map.class)).get("uid");
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
