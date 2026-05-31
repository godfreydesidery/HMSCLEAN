package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The discharge gate (PROCESS_MISMATCHES.md M17): an admission cannot be closed
 * by calling the discharge endpoint directly — a discharge plan of the matching
 * kind must be APPROVED first; approving the plan is what closes the admission.
 *
 * Uses the seeded General Ward (V3) and ROOT as the admitting clinician.
 */
class DischargeGateIT extends AuthenticatedIntegrationTest {

    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void dischargeIsRefusedWithoutAnApprovedPlanThenSucceedsViaApproval() {
        String patientUid = stringField(expectOk(post(
                "/patients",
                Map.of("firstName", "Dis", "lastName", "Charge",
                        "dateOfBirth", LocalDate.now().minusYears(50).toString(),
                        "gender", "MALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)), "uid");

        String admissionUid = stringField(expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "observation"),
                Map.class)), "uid");

        // Direct discharge with no plan is refused.
        ResponseEntity<Map> denied = post(
                "/encounters/admissions/uid/" + admissionUid + "/discharge",
                Map.of("summary", "quick"), Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Discharge must be refused without an APPROVED discharge plan").isTrue();

        // Admitting to a priced ward now issues a ward-bed bill that arms the
        // discharge bill-clearance gate (process-audit cluster #1) — settle it so
        // this test exercises the discharge-plan gate, not the bills gate.
        settleAdmissionBill(admissionUid);

        // Create the plan (authored by root), approve as a DIFFERENT user
        // (segregation of duties) → admission closes.
        expectOk(post("/encounters/admissions/uid/" + admissionUid + "/discharge-plan",
                Map.of("kind", "DISCHARGE",
                        "history", "stable throughout",
                        "management", "supportive",
                        "recommendations", "discharge home"),
                Map.class));
        Map plan = expectOk(postAs(secondUserToken(),
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan/approve", null, Map.class));
        assertThat(plan.get("status")).isEqualTo("APPROVED");

        Map admission = expectOk(get("/encounters/admissions/uid/" + admissionUid, Map.class));
        assertThat(admission.get("status")).isEqualTo("DISCHARGED");
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
