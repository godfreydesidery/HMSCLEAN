package com.otapp.hmis.engine.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The pharmacy dispensing queue (PROCESS_MISMATCHES.md M5/M7): prescriptions
 * awaiting pharmacy action surface on a worklist, scoped by patient class —
 * OUTPATIENT (consultation-bound) vs OUTSIDER (raised directly on the patient).
 *
 * Uses the seeded OPD clinic + Amoxil medicine; ROOT as clinician.
 */
class DispenseWorklistIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String AMOXIL_UID     = "01J5KQRPCD0000000000000MD2";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void outpatientPrescriptionIsScopedToOutpatientQueue() {
        String patientUid = registerPatient("Opd", "Script", "OUTPATIENT");
        String consultationUid = stringField(expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "cough"),
                Map.class)), "uid");

        String rxUid = stringField(expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/prescriptions",
                Map.of("medicineUid", AMOXIL_UID, "dose", "1 tab", "frequency", "BD", "quantity", 10),
                Map.class)), "uid");

        // Appears on the unscoped queue and the OUTPATIENT queue, not the OUTSIDER queue.
        assertThat(worklistContains(rxUid, null)).isTrue();
        assertThat(worklistContains(rxUid, "OUTPATIENT")).isTrue();
        assertThat(worklistContains(rxUid, "OUTSIDER"))
                .as("Consultation-bound Rx must not show under the OUTSIDER scope").isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void outsiderPrescriptionIsScopedToOutsiderQueue() {
        String patientUid = registerPatient("Walk", "In", "OUTSIDER");
        String rxUid = stringField(expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-prescriptions",
                Map.of("medicineUid", AMOXIL_UID, "dose", "1 tab", "frequency", "OD", "quantity", 5),
                Map.class)), "uid");

        assertThat(worklistContains(rxUid, "OUTSIDER")).isTrue();
        assertThat(worklistContains(rxUid, "OUTPATIENT"))
                .as("Outsider Rx must not show under the OUTPATIENT scope").isFalse();
    }

    private String registerPatient(String first, String last, String type) {
        return stringField(expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   first,
                        "lastName",    last,
                        "dateOfBirth", LocalDate.now().minusYears(33).toString(),
                        "gender",      "FEMALE",
                        "type",        type,
                        "paymentType", "CASH"),
                Map.class)), "uid");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean worklistContains(String rxUid, String patientClass) {
        String path = "/encounters/prescriptions/worklist?size=200"
                + (patientClass == null ? "" : "&patientClass=" + patientClass);
        Map<String, Object> page = expectOk(get(path, Map.class));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        return content != null && content.stream().anyMatch(r -> rxUid.equals(r.get("uid")));
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
