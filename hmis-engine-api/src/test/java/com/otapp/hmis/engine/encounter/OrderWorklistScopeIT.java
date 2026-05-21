package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Phase 2 of the fidelity work (PROCESS_MISMATCHES.md M8): the lab/radiology/
 * procedure worklist scopes by patient class — OUTPATIENT (consultation-bound)
 * vs OUTSIDER (raised directly on the patient).
 *
 * Uses the seeded OPD clinic + CBC lab test; ROOT as clinician.
 */
class OrderWorklistScopeIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String CBC_UID        = "01J5KQRPCD0000000000000LB1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void outpatientLabOrderIsScopedToOutpatientWorklist() {
        String patientUid = registerPatient("Opd", "Lab", "OUTPATIENT");
        String consultationUid = stringField(expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", "root", "paymentType", "CASH", "reason", "screen"),
                Map.class)), "uid");
        String orderUid = stringField(expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/orders",
                Map.of("kind", "LAB_TEST", "serviceUid", CBC_UID, "urgency", "NORMAL"),
                Map.class)), "uid");

        assertThat(worklistContains(orderUid, "LAB_TEST", "OUTPATIENT")).isTrue();
        assertThat(worklistContains(orderUid, "LAB_TEST", "OUTSIDER"))
                .as("Consultation-bound order must not show under the OUTSIDER scope").isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void outsiderLabOrderIsScopedToOutsiderWorklist() {
        String patientUid = registerPatient("Walk", "LabIn", "OUTSIDER");
        String orderUid = stringField(expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders",
                Map.of("kind", "LAB_TEST", "serviceUid", CBC_UID, "urgency", "NORMAL"),
                Map.class)), "uid");

        assertThat(worklistContains(orderUid, "LAB_TEST", "OUTSIDER")).isTrue();
        assertThat(worklistContains(orderUid, "LAB_TEST", "OUTPATIENT"))
                .as("Outsider order must not show under the OUTPATIENT scope").isFalse();
    }

    private String registerPatient(String first, String last, String type) {
        return stringField(expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   first,
                        "lastName",    last,
                        "dateOfBirth", LocalDate.now().minusYears(36).toString(),
                        "gender",      "MALE",
                        "type",        type,
                        "paymentType", "CASH"),
                Map.class)), "uid");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean worklistContains(String orderUid, String kind, String patientClass) {
        Map<String, Object> page = expectOk(get(
                "/encounters/orders?size=200&kind=" + kind + "&patientClass=" + patientClass, Map.class));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        return content != null && content.stream().anyMatch(r -> orderUid.equals(r.get("uid")));
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
