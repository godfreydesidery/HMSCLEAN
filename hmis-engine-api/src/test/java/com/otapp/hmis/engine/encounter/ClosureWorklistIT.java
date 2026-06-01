package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * Closure worklist (gap audit cluster 3, DISCH-1): the single queue of PENDING
 * closure plans awaiting a second approver, across BOTH subjects — inpatient
 * discharges/deaths/referrals AND outpatient deaths/referrals. Approving (or
 * cancelling) a plan removes it from the worklist.
 *
 * <p>The test container is JVM-lifetime and shared, so other tests may leave
 * their own pending plans in {@code discharge_plan}; assertions are therefore
 * identity-based (does the worklist contain THIS plan's uid?) over a large page,
 * never absolute counts.
 */
class ClosureWorklistIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {};

    @Test
    void worklistListsPendingClosuresAcrossBothSubjectsAndDropsOnApprove() {
        // --- an OUTPATIENT consultation referral closure (PENDING) ----------
        String outPatientUid = registerPatient("Work", "ListOut");
        String consultationUid = bookAndOpenConsultation(outPatientUid);
        String consultPlanUid = (String) expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/closure",
                Map.ofEntries(
                        Map.entry("kind", "REFERRAL"),
                        Map.entry("history", "seen for fever"),
                        Map.entry("management", "antipyretics"),
                        Map.entry("recommendations", "specialist review"),
                        Map.entry("referralFacility", "District Hospital"),
                        Map.entry("referralReason", "no isolation ward")),
                Map.class)).get("uid");

        // --- an INPATIENT discharge closure (PENDING) -----------------------
        String inPatientUid = registerPatient("Work", "ListIn");
        String admissionUid = (String) expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", inPatientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "observation"),
                Map.class)).get("uid");
        settleAdmissionBill(admissionUid);
        String admissionPlanUid = (String) expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/discharge-plan",
                Map.ofEntries(
                        Map.entry("kind", "DISCHARGE"),
                        Map.entry("history", "stable overnight"),
                        Map.entry("management", "IV fluids"),
                        Map.entry("recommendations", "discharge home")),
                Map.class)).get("uid");

        // --- the combined worklist holds both, with patient + subject detail -
        List<Map<String, Object>> all = worklist(null);
        Map<String, Object> consultRow = findByUid(all, consultPlanUid);
        Map<String, Object> admissionRow = findByUid(all, admissionPlanUid);
        assertThat(consultRow).as("consultation closure on the worklist").isNotNull();
        assertThat(admissionRow).as("admission closure on the worklist").isNotNull();

        assertThat(consultRow.get("subjectType")).isEqualTo("CONSULTATION");
        assertThat(consultRow.get("kind")).isEqualTo("REFERRAL");
        assertThat(consultRow.get("status")).isEqualTo("PENDING");
        assertThat(consultRow.get("consultationUid")).isEqualTo(consultationUid);
        assertThat((String) consultRow.get("patientName")).contains("ListOut");
        assertThat(consultRow.get("patientNo")).isNotNull();

        assertThat(admissionRow.get("subjectType")).isEqualTo("ADMISSION");
        assertThat(admissionRow.get("kind")).isEqualTo("DISCHARGE");
        assertThat(admissionRow.get("admissionUid")).isEqualTo(admissionUid);
        assertThat((String) admissionRow.get("patientName")).contains("ListIn");

        // --- subjectType filter scopes the queue ----------------------------
        List<Map<String, Object>> consultationsOnly = worklist("CONSULTATION");
        assertThat(findByUid(consultationsOnly, consultPlanUid)).isNotNull();
        assertThat(findByUid(consultationsOnly, admissionPlanUid)).isNull();

        List<Map<String, Object>> admissionsOnly = worklist("ADMISSION");
        assertThat(findByUid(admissionsOnly, admissionPlanUid)).isNotNull();
        assertThat(findByUid(admissionsOnly, consultPlanUid)).isNull();

        // --- approving drops the plan from the worklist ---------------------
        expectOk(postAs(secondUserToken(),
                "/encounters/consultations/uid/" + consultationUid + "/closure/approve", null, Map.class));
        List<Map<String, Object>> afterApprove = worklist(null);
        assertThat(findByUid(afterApprove, consultPlanUid))
                .as("approved closure leaves the worklist").isNull();
        assertThat(findByUid(afterApprove, admissionPlanUid))
                .as("the still-pending admission closure remains").isNotNull();
    }

    // ----- helpers ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> worklist(String subjectType) {
        String path = "/encounters/closures/worklist?size=200"
                + (subjectType == null ? "" : "&subjectType=" + subjectType);
        ResponseEntity<Map<String, Object>> resp = get(path, MAP);
        Map<String, Object> body = expectOk(resp);
        return (List<Map<String, Object>>) body.get("content");
    }

    private static Map<String, Object> findByUid(List<Map<String, Object>> rows, String uid) {
        return rows.stream()
                .filter(r -> uid.equals(r.get("uid")))
                .findFirst()
                .orElse(null);
    }

    private String registerPatient(String first, String last) {
        return (String) expectOk(post(
                "/patients",
                Map.of("firstName", first, "lastName", last,
                        "dateOfBirth", LocalDate.now().minusYears(35).toString(),
                        "gender", "FEMALE", "type", "OUTPATIENT", "paymentType", "CASH"),
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
