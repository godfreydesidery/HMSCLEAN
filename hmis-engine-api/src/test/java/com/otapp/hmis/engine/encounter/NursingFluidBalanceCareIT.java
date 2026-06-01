package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Fluid-balance + care-activity nursing charts (gap audit ADMIT-1 / ADMIT-2):
 * record intake/output and per-shift care activities against an admission, list
 * them back, and assert the per-entry output/net is computed. Admits CASH to the
 * unpriced General Ward (WG1) so the admission activates immediately with no
 * deposit gate.
 */
class NursingFluidBalanceCareIT extends AuthenticatedIntegrationTest {

    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void recordsFluidBalanceAndCareActivityAgainstAnAdmission() {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of("firstName", "Flu", "lastName", "Idbalance",
                        "dateOfBirth", LocalDate.now().minusYears(40).toString(),
                        "gender", "MALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)).get("uid");

        String admissionUid = (String) expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root",
                        "paymentType", "CASH", "admissionReason", "observation"),
                Map.class)).get("uid");
        assertThat(expectOk(get("/encounters/admissions/uid/" + admissionUid, Map.class)).get("status"))
                .isEqualTo("ADMITTED");

        // ----- fluid balance: intake 500, urine 200, drainage 50 -> output 250, net +250
        Map fb = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/fluid-balance",
                Map.of("intakeMl", 500, "urineOutputMl", 200, "drainageOutputMl", 50, "notes", "morning round"),
                Map.class));
        assertThat(fb.get("outputMl")).isEqualTo(250);
        assertThat(fb.get("netMl")).isEqualTo(250);

        // A second, output-only entry (net negative).
        Map fb2 = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/fluid-balance",
                Map.of("urineOutputMl", 300),
                Map.class));
        assertThat(fb2.get("netMl")).isEqualTo(-300);

        List<Map<String, Object>> fbList = expectOk(get(
                "/encounters/admissions/uid/" + admissionUid + "/fluid-balance", List.class));
        assertThat(fbList).hasSize(2);

        // An empty fluid-balance entry is rejected (must record at least one figure).
        assertThat(post("/encounters/admissions/uid/" + admissionUid + "/fluid-balance",
                Map.of("notes", "nothing measured"), Map.class)
                .getStatusCode().is4xxClientError()).isTrue();

        // ----- care activity: feeding + repositioning + a random BG reading
        Map ca = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/care-activities",
                Map.of("feedingDone", true, "positionChanged", true, "bedBathDone", false,
                        "randomBloodSugarMmol", "6.40", "notes", "tolerated feeds"),
                Map.class));
        assertThat(ca.get("feedingDone")).isEqualTo(true);
        assertThat(ca.get("positionChanged")).isEqualTo(true);
        assertThat(ca.get("bedBathDone")).isEqualTo(false);

        List<Map<String, Object>> caList = expectOk(get(
                "/encounters/admissions/uid/" + admissionUid + "/care-activities", List.class));
        assertThat(caList).hasSize(1);

        // An entirely-empty care-activity entry is rejected.
        assertThat(post("/encounters/admissions/uid/" + admissionUid + "/care-activities",
                Map.of("feedingDone", false, "positionChanged", false, "bedBathDone", false), Map.class)
                .getStatusCode().is4xxClientError()).isTrue();
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
