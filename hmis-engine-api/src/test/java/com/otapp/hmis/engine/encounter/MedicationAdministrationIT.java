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
 * The nursing medication-administration record / MAR (PROCESS_MISMATCHES.md M15):
 * a nurse charts each bedside dose against an admission's prescription, and the
 * record captures who administered it.
 *
 * Uses the seeded OPD clinic, Amoxil medicine, and General Ward; ROOT as the actor.
 */
class MedicationAdministrationIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String AMOXIL_UID     = "01J5KQRPCD0000000000000MD2";
    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void nurseChartsAdministeredDoseAgainstPrescription() {
        String patientUid = stringField(expectOk(post(
                "/patients",
                Map.of("firstName", "Mar", "lastName", "Patient",
                        "dateOfBirth", LocalDate.now().minusYears(45).toString(),
                        "gender", "FEMALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)), "uid");

        String consultationUid = stringField(expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", "root", "paymentType", "CASH", "reason", "infection"),
                Map.class)), "uid");

        String prescriptionUid = stringField(expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/prescriptions",
                Map.of("medicineUid", AMOXIL_UID, "dose", "500 mg", "frequency", "TDS", "quantity", 21),
                Map.class)), "uid");

        String admissionUid = stringField(expectOk(post(
                "/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", GENERAL_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "consultationUid", consultationUid, "admissionReason", "IV antibiotics"),
                Map.class)), "uid");

        // Record an administered dose.
        Map record = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/medication-administrations",
                Map.of("prescriptionUid", prescriptionUid, "doseGiven", "500 mg",
                        "route", "PO", "patientResponse", "tolerated well"),
                Map.class));
        assertThat(record.get("doseGiven")).isEqualTo("500 mg");
        assertThat(record.get("administeredByUsername")).isEqualTo("root");
        assertThat(record.get("prescriptionUid")).isEqualTo(prescriptionUid);

        // It appears on the MAR.
        ResponseEntity<List<Map<String, Object>>> list = get(
                "/encounters/admissions/uid/" + admissionUid + "/medication-administrations",
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertThat(list.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(list.getBody())
                .anySatisfy(m -> assertThat(m.get("prescriptionUid")).isEqualTo(prescriptionUid));
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
