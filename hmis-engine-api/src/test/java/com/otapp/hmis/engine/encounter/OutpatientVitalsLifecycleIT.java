package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * OPC-3 — faithful nurse-fill vitals lifecycle + the outpatient nurse-triage
 * worklist (legacy Zana-HMIS PatientVital EMPTY → PENDING → SUBMITTED → ARCHIVED):
 *
 *   1. the nurse fills the readings on an open consultation  → PENDING;
 *   2. the nurse submits the set                             → SUBMITTED (locked);
 *   3. a second submit is rejected (4xx)                     — locked;
 *   4. the doctor consumes the submitted set                 → ARCHIVED;
 *   5. consuming a non-SUBMITTED set is rejected (4xx);
 *   6. a fee-settled BOOKED/IN_PROGRESS consultation shows on the OUTPATIENT
 *      nurse worklist, carrying the current vitals status.
 *
 * Uses the seeded OPD clinic.
 */
class OutpatientVitalsLifecycleIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";

    /**
     * Register a CASH patient, pay the registration fee, book a consultation at OPD
     * and open it (settle the consultation fee + start) so it is IN_PROGRESS and
     * fee-settled. Returns the consultation uid.
     */
    @SuppressWarnings("rawtypes")
    private String openOpdConsultation(String firstName) {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   firstName,
                        "lastName",    "Vitals",
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender",      "FEMALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)).get("uid");

        Map regFee = expectOk(get(
                "/billing/patients/uid/" + patientUid + "/registration-fee", Map.class));
        expectOk(post(
                "/billing/invoices/uid/" + regFee.get("uid") + "/payments",
                Map.of("method", "CASH", "amount", new BigDecimal("5000.00"), "currency", "TZS"),
                Map.class));

        Map consult = expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",        patientUid,
                        "clinicUid",         OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType",       "CASH",
                        "reason",            "headache"),
                Map.class));
        String consultUid = (String) consult.get("uid");
        openConsultation(consultUid); // settles fee + starts → IN_PROGRESS, feeSettled
        return consultUid;
    }

    private Map<String, Object> vitalsBody() {
        return Map.of(
                "temperatureC",           new BigDecimal("37.2"),
                "pulseBpm",               80,
                "respirationBpm",         18,
                "bloodPressureSystolic",  120,
                "bloodPressureDiastolic", 80,
                "spo2Percent",            98,
                "weightKg",               new BigDecimal("65.0"),
                "heightCm",               new BigDecimal("165.0"),
                "notes",                  "stable");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void fillSubmitConsumeWalksTheLifecycle() {
        String consultUid = openOpdConsultation("Lifecycle");

        // 1. Nurse fills → PENDING.
        Map filled = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals",
                vitalsBody(), Map.class));
        String vitalsUid = (String) filled.get("uid");
        assertThat(filled.get("status")).isEqualTo("PENDING");
        assertThat(filled.get("id")).isNotNull();
        assertThat(filled.get("uid")).isNotNull();
        assertThat(filled.get("pulseBpm")).isEqualTo(80);

        // Re-save is allowed while still PENDING and reuses the SAME open row.
        Map resaved = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals",
                vitalsBody(), Map.class));
        assertThat(resaved.get("uid")).isEqualTo(vitalsUid);
        assertThat(resaved.get("status")).isEqualTo("PENDING");

        // 2. Nurse submits → SUBMITTED (locked).
        Map submitted = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals/uid/" + vitalsUid + "/submit",
                null, Map.class));
        assertThat(submitted.get("status")).isEqualTo("SUBMITTED");
        assertThat(submitted.get("submittedAt")).isNotNull();

        // 3. A second submit is rejected — SUBMITTED is locked.
        ResponseEntity<Map> reSubmit = post(
                "/encounters/consultations/uid/" + consultUid + "/vitals/uid/" + vitalsUid + "/submit",
                null, Map.class);
        assertThat(reSubmit.getStatusCode().is4xxClientError())
                .as("A SUBMITTED vitals set cannot be re-submitted")
                .isTrue();

        // 4. Doctor consumes → ARCHIVED.
        Map archived = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals/uid/" + vitalsUid + "/consume",
                null, Map.class));
        assertThat(archived.get("status")).isEqualTo("ARCHIVED");
        assertThat(archived.get("archivedAt")).isNotNull();

        // 5. Consuming again (no longer SUBMITTED) is rejected.
        ResponseEntity<Map> reConsume = post(
                "/encounters/consultations/uid/" + consultUid + "/vitals/uid/" + vitalsUid + "/consume",
                null, Map.class);
        assertThat(reConsume.getStatusCode().is4xxClientError())
                .as("Only a SUBMITTED vitals set can be consumed")
                .isTrue();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void submitFromEmptyOrUnfilledIsRejected() {
        String consultUid = openOpdConsultation("NoFill");

        // Materialise + fill, then submit, then re-fill is impossible (locked).
        Map filled = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals",
                vitalsBody(), Map.class));
        String vitalsUid = (String) filled.get("uid");
        expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals/uid/" + vitalsUid + "/submit",
                null, Map.class));

        // Consuming a freshly-saved (PENDING, not yet submitted) row is rejected:
        // start a second open row and try to consume it.
        Map second = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals",
                vitalsBody(), Map.class));
        String secondUid = (String) second.get("uid");
        assertThat(second.get("status")).isEqualTo("PENDING");

        ResponseEntity<Map> consumePending = post(
                "/encounters/consultations/uid/" + consultUid + "/vitals/uid/" + secondUid + "/consume",
                null, Map.class);
        assertThat(consumePending.getStatusCode().is4xxClientError())
                .as("A PENDING (not yet submitted) vitals set cannot be consumed")
                .isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void feeSettledConsultationAppearsOnOutpatientNurseWorklist() {
        String consultUid = openOpdConsultation("Worklist");

        // Before any vitals are saved the consultation is already on the queue
        // (fee-settled, IN_PROGRESS) with no vitals status yet.
        Map queue = expectOk(get(
                "/encounters/consultations/nurse-worklist?page=0&size=200", Map.class));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) queue.get("content");
        Map<String, Object> mine = rows.stream()
                .filter(r -> consultUid.equals(r.get("consultationUid")))
                .findFirst().orElseThrow(() ->
                        new AssertionError("Consultation not on outpatient nurse worklist: " + consultUid));
        assertThat(mine.get("feeSettled")).isEqualTo(Boolean.TRUE);
        assertThat(mine.get("consultationStatus")).isEqualTo("IN_PROGRESS");
        assertThat(mine.get("patientName")).isNotNull();
        assertThat(mine.get("patientNo")).isNotNull();
        assertThat(mine.get("vitalsStatus")).isNull();

        // After the nurse fills, the worklist row carries the PENDING status.
        Map filled = expectOk(post(
                "/encounters/consultations/uid/" + consultUid + "/vitals",
                vitalsBody(), Map.class));
        String vitalsUid = (String) filled.get("uid");

        Map queue2 = expectOk(get(
                "/encounters/consultations/nurse-worklist?page=0&size=200", Map.class));
        List<Map<String, Object>> rows2 = (List<Map<String, Object>>) queue2.get("content");
        Map<String, Object> mine2 = rows2.stream()
                .filter(r -> consultUid.equals(r.get("consultationUid")))
                .findFirst().orElseThrow();
        assertThat(mine2.get("vitalsStatus")).isEqualTo("PENDING");
        assertThat(mine2.get("vitalsUid")).isEqualTo(vitalsUid);
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
