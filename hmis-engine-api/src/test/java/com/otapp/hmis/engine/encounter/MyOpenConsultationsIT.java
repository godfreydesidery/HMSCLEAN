package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * OPC-4 — "my open consultations": a clinician's own active consultations
 * (BOOKED / IN_PROGRESS / TRANSFERRED), scoped to the signed-in clinician.
 *
 *   * a clinician's own BOOKED and IN_PROGRESS consultation appears on their
 *     {@code /my-open};
 *   * another clinician's consultation is NOT visible to them.
 *
 * The queue is JWT/clinician-scoped exactly like {@code /reception-queue}, so it
 * is queried AS the owning clinician (not ROOT). Uses the seeded OPD clinic (V3).
 */
class MyOpenConsultationsIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";

    @Test
    void myOpenShowsOwnActiveConsultationsAndHidesOthers() {
        // --- Clinician A: the shared affiliated clinician. -------------------
        String clinicianA = clinicianAffiliatedWith(OPD_CLINIC_UID);
        String tokenA = sharedClinicianToken();

        // A books two consultations: one left BOOKED, one driven to IN_PROGRESS.
        String patientBooked = registerPatient("Open", "Booked");
        String bookedUid = bookConsultation(patientBooked, clinicianA);

        String patientInProgress = registerPatient("Open", "Active");
        String inProgressUid = bookConsultation(patientInProgress, clinicianA);
        openConsultation(inProgressUid); // settles fee + starts → IN_PROGRESS

        // --- Clinician B: a second, independently provisioned clinician. -----
        String clinicianB = secondClinicianAffiliatedWith(OPD_CLINIC_UID);
        String tokenB = secondClinicianToken(clinicianB);

        String patientOther = registerPatient("Other", "Patient");
        String otherUid = bookConsultation(patientOther, clinicianB);

        // A's /my-open shows both of A's own active consultations...
        List<String> mineForA = myOpenUids(tokenA);
        assertThat(mineForA)
                .as("A's own BOOKED consultation must appear on A's /my-open")
                .contains(bookedUid);
        assertThat(mineForA)
                .as("A's own IN_PROGRESS consultation must appear on A's /my-open")
                .contains(inProgressUid);

        // ...but NOT B's consultation.
        assertThat(mineForA)
                .as("B's consultation must NOT appear on A's /my-open")
                .doesNotContain(otherUid);

        // And B's /my-open shows B's consultation, not A's.
        List<String> mineForB = myOpenUids(tokenB);
        assertThat(mineForB)
                .as("B's own consultation must appear on B's /my-open")
                .contains(otherUid);
        assertThat(mineForB)
                .as("A's consultations must NOT appear on B's /my-open")
                .doesNotContain(bookedUid, inProgressUid);
    }

    // ----- helpers -----------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<String> myOpenUids(String token) {
        Map<String, Object> page = expectOk(getAs(token,
                "/encounters/consultations/my-open?size=200", Map.class));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        return content == null ? List.of()
                : content.stream().map(r -> (String) r.get("uid")).toList();
    }

    @SuppressWarnings("unchecked")
    private String registerPatient(String first, String last) {
        var body = new java.util.HashMap<String, Object>();
        body.put("firstName", first);
        body.put("lastName", last);
        body.put("dateOfBirth", LocalDate.now().minusYears(30).toString());
        body.put("gender", "MALE");
        body.put("type", "OUTPATIENT");
        body.put("paymentType", "INSURANCE");
        body.put("insurancePlanUid", "01J5KQRPCD0000000000000IP1");
        body.put("membershipNo", "M-001");
        Map<String, Object> patient = expectOk(post("/patients", body, Map.class));
        return (String) patient.get("uid");
    }

    /** Books an INSURANCE consultation (covered → fee settles at booking) for the given clinician. */
    @SuppressWarnings("unchecked")
    private String bookConsultation(String patientUid, String clinicianUsername) {
        var body = new java.util.HashMap<String, Object>();
        body.put("patientUid", patientUid);
        body.put("clinicUid", OPD_CLINIC_UID);
        body.put("clinicianUsername", clinicianUsername);
        body.put("paymentType", "INSURANCE");
        body.put("insurancePlanUid", "01J5KQRPCD0000000000000IP1");
        body.put("reason", "fever");
        Map<String, Object> consultation = expectOk(post("/encounters/consultations", body, Map.class));
        return (String) consultation.get("uid");
    }

    private String secondClinicianUsername;

    /**
     * Provisions a SECOND CLINICIAN distinct from the shared one and affiliates it
     * with the clinic, returning its username. The base helper only yields a single
     * shared clinician per test instance; this test needs two to assert scoping.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String secondClinicianAffiliatedWith(String clinicUid) {
        if (secondClinicianUsername == null) {
            String username = "clinB" + Long.toString(System.nanoTime(), 36);
            Map<String, Object> created = expectOk(post(
                    "/iam/users",
                    Map.of(
                            "username", username,
                            "password", "Clinician!123",
                            "firstName", "Second",
                            "lastName", "Clinician",
                            "email", username + "@test.local",
                            "roles", java.util.Set.of("CLINICIAN")),
                    Map.class));
            secondClinicianUsername = username;
            String userUid = (String) created.get("uid");
            ResponseEntity<Map> assigned = post(
                    "/masterdata/clinics/uid/" + clinicUid + "/clinicians",
                    Map.of("userUid", userUid), Map.class);
            assertThat(assigned.getStatusCode().is2xxSuccessful())
                    .as("affiliate second clinician with clinic").isTrue();
        }
        return secondClinicianUsername;
    }

    private String secondClinicianToken(String username) {
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(username, "Clinician!123"), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("Login failed for second clinician " + username);
        }
        return login.tokens().accessToken();
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
