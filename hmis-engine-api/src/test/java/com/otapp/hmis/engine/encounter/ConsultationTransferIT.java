package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Phase 44 — consultation lifecycle additions:
 *
 *   * follow-up flag — booking with {@code followUpOfConsultationUid}
 *     references a prior consultation; mismatched patient is rejected.
 *   * transfer — POST /uid/{uid}/transfer hands the patient off to
 *     another clinic / clinician. The original consultation closes as
 *     TRANSFERRED, a new BOOKED consultation is created at the target,
 *     and both reference each other.
 *
 * Uses the seeded OPD + PED clinics.
 */
class ConsultationTransferIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String PED_CLINIC_UID = "01J5KQRPCD0000000000000CN2";

    @Test
    @SuppressWarnings("rawtypes")
    void followUpAndTransferLinkConsultations() {
        // 1. Register a CASH patient and pay the registration fee so the
        //    booking gate doesn't fire.
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Foll",
                        "lastName",    "Owup",
                        "dateOfBirth", LocalDate.now().minusYears(20).toString(),
                        "gender",      "MALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)).get("uid");

        Map regFee = expectOk(get(
                "/billing/patients/uid/" + patientUid + "/registration-fee",
                Map.class));
        String regInvoiceUid = (String) regFee.get("uid");
        expectOk(post(
                "/billing/invoices/uid/" + regInvoiceUid + "/payments",
                Map.of("method", "CASH", "amount", new BigDecimal("5000.00"), "currency", "TZS"),
                Map.class));

        // 2. Book the first consultation at OPD.
        Map first = expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",         patientUid,
                        "clinicUid",          OPD_CLINIC_UID,
                        "clinicianUsername",  clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType",        "CASH",
                        "reason",             "fever"),
                Map.class));
        String firstUid = (String) first.get("uid");

        // 2b. Release the patient by closing the first visit. The active-consultation
        //     guard refuses a second booking for the same patient while one is still
        //     active, and legacy books a follow-up only once the prior visit is
        //     signed out — so pay + open + complete it before booking the follow-up.
        openConsultation(firstUid);
        expectOk(post("/encounters/consultations/uid/" + firstUid + "/complete", null, Map.class));

        // 3. Book a follow-up to it (allowed now that the first visit is closed).
        Map followUp = expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",                 patientUid,
                        "clinicUid",                  OPD_CLINIC_UID,
                        "clinicianUsername",          clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType",                "CASH",
                        "reason",                     "follow-up",
                        "followUpOfConsultationUid",  firstUid),
                Map.class));
        assertThat(followUp.get("followUpOfConsultationUid")).isEqualTo(firstUid);
        String followUpUid = (String) followUp.get("uid");

        // 4. Transfer the (now-active, BOOKED) follow-up to Pediatrics.
        Map receiver = expectOk(post(
                "/encounters/consultations/uid/" + followUpUid + "/transfer",
                Map.of(
                        "targetClinicUid",         PED_CLINIC_UID,
                        "targetClinicianUsername", clinicianAffiliatedWith(PED_CLINIC_UID),
                        "reason",                  "Better suited to pediatrics"),
                Map.class));
        String receiverUid = (String) receiver.get("uid");
        assertThat(receiver.get("status")).isEqualTo("BOOKED");
        assertThat(receiver.get("clinicUid")).isEqualTo(PED_CLINIC_UID);
        assertThat(receiver.get("transferredFromConsultationUid")).isEqualTo(followUpUid);

        // 5. The transferred follow-up now reads as TRANSFERRED with transferredTo set.
        Map original = expectOk(get(
                "/encounters/consultations/uid/" + followUpUid,
                Map.class));
        assertThat(original.get("status")).isEqualTo("TRANSFERRED");
        assertThat(original.get("transferredToConsultationUid")).isEqualTo(receiverUid);
        assertThat(original.get("transferReason")).isEqualTo("Better suited to pediatrics");

        // 6. Re-transferring an already-TRANSFERRED consultation is refused.
        ResponseEntity<Map> denied = post(
                "/encounters/consultations/uid/" + followUpUid + "/transfer",
                Map.of(
                        "targetClinicUid",         PED_CLINIC_UID,
                        "targetClinicianUsername", clinicianAffiliatedWith(PED_CLINIC_UID)),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("TRANSFERRED is terminal — re-transfer should be refused")
                .isTrue();
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
