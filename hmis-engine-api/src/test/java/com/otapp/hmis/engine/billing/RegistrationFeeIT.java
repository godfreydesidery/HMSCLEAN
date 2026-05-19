package com.otapp.hmis.engine.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Phase 36: the registration-fee + consultation gate.
 *
 *   Registering a patient publishes {@code PatientRegisteredEvent}; billing
 *   listens after-commit and seeds an ISSUED registration invoice. The
 *   {@code ConsultationBookingRequestedEvent} fires synchronously when a
 *   consultation is booked — for CASH patients with an outstanding
 *   registration balance the listener throws and the booking is rejected.
 *
 * Uses the seeded General Outpatient clinic (V3) and the bootstrap ROOT
 * user as the clinician.
 */
class RegistrationFeeIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String NHIF_PLAN_UID  = "01J5KQRPCD0000000000000IP1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void cashPatientCannotBookConsultationUntilRegistrationFeeIsPaid() {
        // 1. Register a CASH OUTPATIENT.
        Map<String, Object> patient = expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Cash",
                        "lastName",    "Customer",
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender",      "FEMALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class));
        String patientUid = (String) patient.get("uid");

        // 2. Registration invoice was seeded by the after-commit listener.
        Map<String, Object> regInvoice = expectOk(get(
                "/billing/patients/uid/" + patientUid + "/registration-fee",
                Map.class));
        assertThat(regInvoice.get("scope")).isEqualTo("REGISTRATION");
        assertThat(regInvoice.get("status")).isEqualTo("ISSUED");
        assertThat(new BigDecimal(regInvoice.get("subtotal").toString()))
                .as("Registration fee picks up the V39-seeded cash price")
                .isEqualByComparingTo("5000.00");
        String invoiceUid = (String) regInvoice.get("uid");

        // 3. Consultation booking refused — registration fee outstanding.
        ResponseEntity<Map> denied = post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",         patientUid,
                        "clinicUid",          OPD_CLINIC_UID,
                        "clinicianUsername",  "root",
                        "paymentType",        "CASH",
                        "reason",             "fever"),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Booking should fail while the registration fee is unpaid")
                .isTrue();

        // 4. Pay the registration fee in full.
        Map<String, Object> paid = expectOk(post(
                "/billing/invoices/uid/" + invoiceUid + "/payments",
                Map.of(
                        "method",   "CASH",
                        "amount",   new BigDecimal("5000.00"),
                        "currency", "TZS"),
                Map.class));
        assertThat(paid.get("status")).isEqualTo("PAID");

        // 5. Now consultation booking succeeds.
        Map<String, Object> consultation = expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",         patientUid,
                        "clinicUid",          OPD_CLINIC_UID,
                        "clinicianUsername",  "root",
                        "paymentType",        "CASH",
                        "reason",             "fever"),
                Map.class));
        assertThat(consultation.get("status")).isEqualTo("BOOKED");
        assertThat(consultation.get("patientUid")).isEqualTo(patientUid);
    }

    @Test
    @SuppressWarnings("unchecked")
    void insurancePatientIsNotGatedEvenIfRegistrationIsUnpaid() {
        // INSURANCE patient — registration invoice is still seeded but the gate
        // ignores non-CASH payment types.
        Map<String, Object> patient = expectOk(post(
                "/patients",
                Map.of(
                        "firstName",      "Nhif",
                        "lastName",       "Member",
                        "dateOfBirth",    LocalDate.now().minusYears(35).toString(),
                        "gender",         "MALE",
                        "type",           "OUTPATIENT",
                        "paymentType",    "INSURANCE",
                        "insurancePlanUid", NHIF_PLAN_UID,
                        "membershipNo",   "M-12345"),
                Map.class));
        String patientUid = (String) patient.get("uid");

        Map<String, Object> regInvoice = expectOk(get(
                "/billing/patients/uid/" + patientUid + "/registration-fee",
                Map.class));
        assertThat(regInvoice.get("scope")).isEqualTo("REGISTRATION");

        // Booking with INSURANCE — should succeed despite the unpaid registration
        // fee because the gate only fires for CASH patients.
        Map<String, Object> consultation = expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",         patientUid,
                        "clinicUid",          OPD_CLINIC_UID,
                        "clinicianUsername",  "root",
                        "paymentType",        "INSURANCE",
                        "insurancePlanUid",   NHIF_PLAN_UID,
                        "reason",             "annual check-up"),
                Map.class));
        assertThat(consultation.get("status")).isEqualTo("BOOKED");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void registrationFeeEndpointIsIdempotent() {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Idem",
                        "lastName",    "Potent",
                        "dateOfBirth", LocalDate.now().minusYears(28).toString(),
                        "gender",      "MALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)).get("uid");

        Map first  = expectOk(get("/billing/patients/uid/" + patientUid + "/registration-fee", Map.class));
        Map second = expectOk(post("/billing/patients/uid/" + patientUid + "/registration-fee", null, Map.class));

        assertThat(second.get("uid"))
                .as("Second POST should return the existing invoice, not create a new one")
                .isEqualTo(first.get("uid"));
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body)
                .as("2xx response had null body. Status=%s, Headers=%s",
                        response.getStatusCode(), response.getHeaders())
                .isNotNull();
        return body;
    }
}
