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
 * The legacy "send to doctor → consultation auto-bill → doctor picks it up"
 * flow with the consultation-fee gate (PROCESS_MISMATCHES.md M1/M3/M9/M10):
 *
 *   * booking a consultation seeds an ISSUED CONSULTATION-scope invoice;
 *   * a CASH consultation is hidden from the reception queue and cannot be
 *     opened until the fee is paid;
 *   * an INSURANCE consultation is "covered" — queued and openable immediately;
 *   * a follow-up consultation fee is waived (zero) and settles at booking.
 *
 * Uses the seeded OPD clinic (V3), the NHIF plan (V5), and ROOT as clinician.
 */
class SendToDoctorFlowIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String NHIF_PLAN_UID  = "01J5KQRPCD0000000000000IP1";
    private static final BigDecimal FEE = new BigDecimal("10000.00");

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void cashConsultationIsGatedUntilFeeIsPaid() {
        ensureConsultationFee();
        String patientUid = registerPatient("Gated", "Cash", "CASH", null);

        Map<String, Object> consultation = bookConsultation(patientUid, "CASH", null, null);
        assertThat(consultation.get("status")).isEqualTo("BOOKED");
        String consultationUid = (String) consultation.get("uid");

        // The consultation-fee invoice was seeded at booking (after-commit).
        Map<String, Object> invoice = expectOk(get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        assertThat(invoice.get("scope")).isEqualTo("CONSULTATION");
        assertThat(invoice.get("status")).isEqualTo("ISSUED");
        assertThat(new BigDecimal(invoice.get("subtotal").toString())).isEqualByComparingTo(FEE);
        String invoiceUid = (String) invoice.get("uid");

        // Unpaid CASH → hidden from the reception queue and not openable.
        assertThat(receptionQueueContains(consultationUid))
                .as("Unpaid CASH consultation must be hidden from the reception queue").isFalse();
        ResponseEntity<Map> denied = post(
                "/encounters/consultations/uid/" + consultationUid + "/start", null, Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Opening must be refused until the consultation fee is settled").isTrue();

        // Pay the fee → now visible and openable.
        Map<String, Object> paid = expectOk(post(
                "/billing/invoices/uid/" + invoiceUid + "/payments",
                Map.of("method", "CASH", "amount", FEE, "currency", "TZS"), Map.class));
        assertThat(paid.get("status")).isEqualTo("PAID");

        assertThat(receptionQueueContains(consultationUid))
                .as("Settled CASH consultation appears in the reception queue").isTrue();
        Map<String, Object> started = expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/start", null, Map.class));
        assertThat(started.get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void insuranceConsultationIsCoveredImmediately() {
        ensureConsultationFee();
        String patientUid = registerPatient("Covered", "Member", "INSURANCE", NHIF_PLAN_UID);

        Map<String, Object> consultation = bookConsultation(patientUid, "INSURANCE", NHIF_PLAN_UID, null);
        String consultationUid = (String) consultation.get("uid");

        // No payment recorded, yet it is queued and openable (legacy COVERED).
        assertThat(receptionQueueContains(consultationUid))
                .as("Insurance consultation is covered — queued immediately").isTrue();
        Map<String, Object> started = expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/start", null, Map.class));
        assertThat(started.get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUpConsultationFeeIsWaived() {
        ensureConsultationFee();
        String patientUid = registerPatient("Follow", "Up", "CASH", null);

        // First visit: pay, open, complete so it can be a follow-up source.
        Map<String, Object> first = bookConsultation(patientUid, "CASH", null, null);
        String firstUid = (String) first.get("uid");
        Map<String, Object> firstInvoice = expectOk(get(
                "/billing/consultations/uid/" + firstUid + "/invoice", Map.class));
        expectOk(post("/billing/invoices/uid/" + firstInvoice.get("uid") + "/payments",
                Map.of("method", "CASH", "amount", FEE, "currency", "TZS"), Map.class));
        expectOk(post("/encounters/consultations/uid/" + firstUid + "/start", null, Map.class));
        expectOk(post("/encounters/consultations/uid/" + firstUid + "/complete", null, Map.class));

        // Follow-up: fee waived (zero) → settled at booking, openable with no payment.
        Map<String, Object> followUp = bookConsultation(patientUid, "CASH", null, firstUid);
        String followUpUid = (String) followUp.get("uid");
        Map<String, Object> fuInvoice = expectOk(get(
                "/billing/consultations/uid/" + followUpUid + "/invoice", Map.class));
        assertThat(new BigDecimal(fuInvoice.get("subtotal").toString()))
                .as("Follow-up consultation fee is waived").isEqualByComparingTo("0.00");

        assertThat(receptionQueueContains(followUpUid)).isTrue();
        Map<String, Object> started = expectOk(post(
                "/encounters/consultations/uid/" + followUpUid + "/start", null, Map.class));
        assertThat(started.get("status")).isEqualTo("IN_PROGRESS");
    }

    // ----- helpers -----------------------------------------------------------

    /** Upsert a cash consultation fee for the OPD clinic so the CASH gate has teeth. */
    @SuppressWarnings("rawtypes")
    private void ensureConsultationFee() {
        ResponseEntity<Map> r = put("/masterdata/service-prices",
                Map.of("kind", "CONSULTATION", "serviceUid", OPD_CLINIC_UID,
                        "amount", FEE, "currency", "TZS"),
                Map.class);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String registerPatient(String first, String last, String paymentType, String planUid) {
        var body = new java.util.HashMap<String, Object>();
        body.put("firstName", first);
        body.put("lastName", last);
        body.put("dateOfBirth", LocalDate.now().minusYears(30).toString());
        body.put("gender", "MALE");
        body.put("type", "OUTPATIENT");
        body.put("paymentType", paymentType);
        if (planUid != null) { body.put("insurancePlanUid", planUid); body.put("membershipNo", "M-001"); }
        Map<String, Object> patient = expectOk(post("/patients", body, Map.class));
        return (String) patient.get("uid");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> bookConsultation(String patientUid, String paymentType,
                                                 String planUid, String followUpOf) {
        var body = new java.util.HashMap<String, Object>();
        body.put("patientUid", patientUid);
        body.put("clinicUid", OPD_CLINIC_UID);
        body.put("clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID));
        body.put("paymentType", paymentType);
        if (planUid != null) body.put("insurancePlanUid", planUid);
        if (followUpOf != null) body.put("followUpOfConsultationUid", followUpOf);
        body.put("reason", "fever");
        return expectOk(post("/encounters/consultations", body, Map.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean receptionQueueContains(String consultationUid) {
        Map<String, Object> page = expectOk(get(
                "/encounters/consultations/reception-queue?size=200", Map.class));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        return content != null && content.stream().anyMatch(r -> consultationUid.equals(r.get("uid")));
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
