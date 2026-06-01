package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Closure self-approval config gate (gap audit cluster 3, DISCH-7): the
 * author≠approver control on a closure plan is config-gated. With
 * {@code hmis.closure.allow-self-approval=true} (solo-clinician sites) the
 * author of a plan may also approve it — the four-eyes block is lifted.
 *
 * <p>The default-config (block ON) path is covered by
 * {@link ClosureFoundationIT} and {@link ClosureWorklistIT}; this class flips
 * the flag via {@link TestPropertySource} to exercise the relaxed path.
 */
@TestPropertySource(properties = "hmis.closure.allow-self-approval=true")
class ClosureSelfApprovalConfigIT extends AuthenticatedIntegrationTest {

    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    @Test
    @SuppressWarnings("rawtypes")
    void authorMayApproveOwnPlanWhenSelfApprovalAllowed() {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of("firstName", "Solo", "lastName", "Doctor",
                        "dateOfBirth", LocalDate.now().minusYears(50).toString(),
                        "gender", "MALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)).get("uid");

        String admissionUid = (String) expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "observation"),
                Map.class)).get("uid");
        settleAdmissionBill(admissionUid);

        // ROOT authors the discharge plan...
        expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan",
                Map.ofEntries(
                        Map.entry("kind", "DISCHARGE"),
                        Map.entry("history", "stable"),
                        Map.entry("management", "rest"),
                        Map.entry("recommendations", "discharge home")),
                Map.class));

        // ...and ROOT (the same author) approves it — allowed by the config flag.
        Map approved = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan/approve", null, Map.class));
        assertThat(approved.get("status")).isEqualTo("APPROVED");
        assertThat(approved.get("approvedByUsername")).isEqualTo("root");

        Map admission = expectOk(get("/encounters/admissions/uid/" + admissionUid, Map.class));
        assertThat(admission.get("status")).isEqualTo("DISCHARGED");
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
