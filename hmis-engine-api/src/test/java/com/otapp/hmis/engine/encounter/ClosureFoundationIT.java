package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Closure foundation (gap audit cluster 2):
 *
 *   * DISCH-3 — an OUTPATIENT consultation can be closed as REFERRED (external
 *     referral) or DECEASED via an authored-then-approved closure plan, the
 *     consultation-keyed equivalent of an inpatient discharge plan.
 *   * DISCH-5 — a REFERRAL closure can point at an ExternalMedicalProvider master
 *     row; its name is denormalised onto the plan.
 *   * DISCH-4 — a DECEASED closure (inpatient or outpatient) flags the patient
 *     deceased so they can no longer be booked / admitted.
 *
 * Uses the seeded OPD clinic + General Ward.
 */
class ClosureFoundationIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    @Test
    @SuppressWarnings("rawtypes")
    void outpatientReferralClosesConsultationViaApprovedPlan() {
        String patientUid = registerOutpatient("Refer", "Out");

        // External provider master (DISCH-5).
        String code = "EXT" + Long.toString(System.nanoTime(), 36).toUpperCase();
        Map provider = expectOk(post("/masterdata/external-providers",
                Map.of("code", code, "name", "St. Mary Referral Hospital",
                        "telephone", "+255700000000"),
                Map.class));
        String providerUid = (String) provider.get("uid");

        String consultationUid = bookAndOpenConsultation(patientUid);

        // Author the REFERRAL closure plan (by ROOT), pointing at the provider.
        Map plan = expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/closure",
                Map.ofEntries(
                        Map.entry("kind", "REFERRAL"),
                        Map.entry("history", "seen for chest pain"),
                        Map.entry("management", "stabilised"),
                        Map.entry("recommendations", "needs cardiology"),
                        Map.entry("externalProviderUid", providerUid),
                        Map.entry("referralReason", "no cardiac unit on site")),
                Map.class));
        assertThat(plan.get("subjectType")).isEqualTo("CONSULTATION");
        assertThat(plan.get("status")).isEqualTo("PENDING");
        // The provider name is denormalised onto referralFacility.
        assertThat(plan.get("referralFacility")).isEqualTo("St. Mary Referral Hospital");
        assertThat(plan.get("externalProviderUid")).isEqualTo(providerUid);

        // Self-approval is refused — the author (ROOT) cannot approve.
        ResponseEntity<Map> selfApprove = post(
                "/encounters/consultations/uid/" + consultationUid + "/closure/approve", null, Map.class);
        assertThat(selfApprove.getStatusCode().is4xxClientError())
                .as("A plan cannot be approved by its author").isTrue();

        // A second user approves → consultation closes as REFERRED.
        Map approved = expectOk(postAs(secondUserToken(),
                "/encounters/consultations/uid/" + consultationUid + "/closure/approve", null, Map.class));
        assertThat(approved.get("status")).isEqualTo("APPROVED");

        Map consultation = expectOk(get("/encounters/consultations/uid/" + consultationUid, Map.class));
        assertThat(consultation.get("status")).isEqualTo("REFERRED");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void outpatientDeathFlagsPatientAndBlocksReentry() {
        String patientUid = registerOutpatient("Dec", "Eased");
        String consultationUid = bookAndOpenConsultation(patientUid);

        expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/closure",
                Map.ofEntries(
                        Map.entry("kind", "DECEASED"),
                        Map.entry("history", "cardiac arrest on arrival"),
                        Map.entry("management", "CPR attempted"),
                        Map.entry("recommendations", "post-mortem"),
                        Map.entry("timeOfDeath", Instant.now().toString()),
                        Map.entry("causeOfDeath", "myocardial infarction")),
                Map.class));

        Map approved = expectOk(postAs(secondUserToken(),
                "/encounters/consultations/uid/" + consultationUid + "/closure/approve", null, Map.class));
        assertThat(approved.get("status")).isEqualTo("APPROVED");

        Map consultation = expectOk(get("/encounters/consultations/uid/" + consultationUid, Map.class));
        assertThat(consultation.get("status")).isEqualTo("DECEASED");

        // Patient is flagged deceased (DISCH-4).
        Map patient = expectOk(get("/patients/uid/" + patientUid, Map.class));
        assertThat(patient.get("deceased")).isEqualTo(true);

        // Re-booking and admission are both refused for a deceased patient.
        ResponseEntity<Map> rebook = post("/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "again"),
                Map.class);
        assertThat(rebook.getStatusCode().is4xxClientError())
                .as("A deceased patient cannot be re-booked").isTrue();

        ResponseEntity<Map> admit = post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "obs"),
                Map.class);
        assertThat(admit.getStatusCode().is4xxClientError())
                .as("A deceased patient cannot be admitted").isTrue();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void inpatientDeathFlagsPatient() {
        String patientUid = registerOutpatient("Inp", "Death");

        String admissionUid = (String) expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "critical"),
                Map.class)).get("uid");
        settleAdmissionBill(admissionUid);

        expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan",
                Map.ofEntries(
                        Map.entry("kind", "DECEASED"),
                        Map.entry("history", "deteriorated overnight"),
                        Map.entry("management", "ICU care"),
                        Map.entry("recommendations", "mortuary"),
                        Map.entry("timeOfDeath", Instant.now().toString()),
                        Map.entry("causeOfDeath", "septic shock")),
                Map.class));

        Map approved = expectOk(postAs(secondUserToken(),
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan/approve", null, Map.class));
        assertThat(approved.get("status")).isEqualTo("APPROVED");

        Map admission = expectOk(get("/encounters/admissions/uid/" + admissionUid, Map.class));
        assertThat(admission.get("status")).isEqualTo("DECEASED");

        Map patient = expectOk(get("/patients/uid/" + patientUid, Map.class));
        assertThat(patient.get("deceased")).isEqualTo(true);
    }

    // ----- helpers ---------------------------------------------------------

    private String registerOutpatient(String first, String last) {
        return (String) expectOk(post(
                "/patients",
                Map.of("firstName", first, "lastName", last,
                        "dateOfBirth", LocalDate.now().minusYears(40).toString(),
                        "gender", "MALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)).get("uid");
    }

    private String bookAndOpenConsultation(String patientUid) {
        String consultationUid = (String) expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "assessment"),
                Map.class)).get("uid");
        openConsultation(consultationUid);
        return consultationUid;
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
