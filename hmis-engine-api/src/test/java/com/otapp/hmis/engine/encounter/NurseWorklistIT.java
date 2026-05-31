package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The nurse worklist (PROCESS_MISMATCHES.md M6): currently-ADMITTED patients
 * appear; the ward filter scopes them; discharge drops them off.
 *
 * Uses the seeded General Ward (V3) and ROOT as the admitting clinician.
 */
class NurseWorklistIT extends AuthenticatedIntegrationTest {

    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void admittedPatientsAppearAndDischargeDropsThem() {
        String patientUid = registerOutpatient("Ward", "Patient");

        Map<String, Object> admission = expectOk(post(
                "/encounters/admissions",
                Map.of(
                        "patientUid",                 patientUid,
                        "wardUid",                    GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root",
                        "paymentType",                "CASH",
                        "admissionReason",            "observation"),
                Map.class));
        String admissionUid = (String) admission.get("uid");
        assertThat(admission.get("status")).isEqualTo("ADMITTED");

        // Appears on the worklist, and under the correct ward filter.
        assertThat(nurseWorklistContains(admissionUid, null)).isTrue();
        assertThat(nurseWorklistContains(admissionUid, GENERAL_WARD_UID)).isTrue();

        // Admitting to a priced ward now issues a ward-bed bill that arms the
        // discharge bill-clearance gate (process-audit cluster #1) — settle it so
        // approval can close the admission.
        settleAdmissionBill(admissionUid);

        // Discharge goes through an APPROVED discharge plan (legacy gate, M17):
        // create the plan with the required fields, then approve it as a
        // different user (segregation of duties) — approval closes the admission.
        expectOk(post("/encounters/admissions/uid/" + admissionUid + "/discharge-plan",
                Map.of("kind", "DISCHARGE",
                        "history", "admitted for observation",
                        "management", "supportive care",
                        "recommendations", "rest, follow up in 1 week"),
                Map.class));
        expectOk(postAs(secondUserToken(),
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan/approve",
                null, Map.class));

        assertThat(nurseWorklistContains(admissionUid, null))
                .as("Discharged admission must drop off the nurse worklist").isFalse();
    }

    private String registerOutpatient(String first, String last) {
        return stringField(expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   first,
                        "lastName",    last,
                        "dateOfBirth", LocalDate.now().minusYears(40).toString(),
                        "gender",      "MALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)), "uid");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean nurseWorklistContains(String admissionUid, String wardUid) {
        String path = "/encounters/admissions/nurse-worklist?size=200"
                + (wardUid == null ? "" : "&wardUid=" + wardUid);
        Map<String, Object> page = expectOk(get(path, Map.class));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        return content != null && content.stream().anyMatch(r -> admissionUid.equals(r.get("uid")));
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
