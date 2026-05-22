package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The legacy fidelity gate: a consultation may only be routed to a clinician who
 * holds the CLINICIAN role AND is affiliated with the chosen clinic. The same
 * gate applies to the receiving clinic on transfer.
 */
class BookingAffiliationGateIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String PED_CLINIC_UID = "01J5KQRPCD0000000000000CN2";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unassignedClinicianIsRejectedThenAcceptedOnceAffiliated() {
        String patientUid = registerPatient();
        String[] clinician = createUser("doc", "CLINICIAN");

        // Enabled CLINICIAN but NOT affiliated with the clinic → rejected.
        ResponseEntity<Map> denied = bookAs(patientUid, OPD_CLINIC_UID, clinician[1]);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Booking an unaffiliated clinician must be refused").isTrue();

        // Affiliate, then booking succeeds.
        assignClinician(OPD_CLINIC_UID, clinician[0]);
        ResponseEntity<Map> ok = bookAs(patientUid, OPD_CLINIC_UID, clinician[1]);
        assertThat(ok.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(ok.getBody().get("status")).isEqualTo("BOOKED");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void nonClinicianUserIsRejected() {
        String patientUid = registerPatient();
        String[] nurse = createUser("nurse", "NURSE");

        ResponseEntity<Map> denied = bookAs(patientUid, OPD_CLINIC_UID, nurse[1]);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("A non-clinician cannot be booked").isTrue();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void transferEnforcesMembershipOnReceivingClinic() {
        String patientUid = registerPatient();
        String[] clinician = createUser("doc", "CLINICIAN");
        assignClinician(OPD_CLINIC_UID, clinician[0]);

        Map<String, Object> booked = bookAs(patientUid, OPD_CLINIC_UID, clinician[1]).getBody();
        String consultationUid = (String) booked.get("uid");

        // Transfer to PED with the same clinician, who is NOT affiliated with PED → rejected.
        ResponseEntity<Map> denied = post(
                "/encounters/consultations/uid/" + consultationUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID,
                        "targetClinicianUsername", clinician[1], "reason", "specialist"),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Transfer to a clinic the clinician is not affiliated with must be refused").isTrue();

        // Affiliate with PED, then the transfer succeeds.
        assignClinician(PED_CLINIC_UID, clinician[0]);
        ResponseEntity<Map> ok = post(
                "/encounters/consultations/uid/" + consultationUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID,
                        "targetClinicianUsername", clinician[1], "reason", "specialist"),
                Map.class);
        assertThat(ok.getStatusCode().is2xxSuccessful()).isTrue();
    }

    // ----- helpers -----------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<Map> bookAs(String patientUid, String clinicUid, String clinicianUsername) {
        return post("/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", clinicUid,
                        "clinicianUsername", clinicianUsername, "paymentType", "CASH",
                        "reason", "fever"),
                Map.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assignClinician(String clinicUid, String clinicianUid) {
        ResponseEntity<Map> r = post(
                "/masterdata/clinics/uid/" + clinicUid + "/clinicians",
                Map.of("userUid", clinicianUid), Map.class);
        assertThat(r.getStatusCode().is2xxSuccessful()).as("assign clinician").isTrue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String registerPatient() {
        String suffix = Long.toString(System.nanoTime(), 36);
        ResponseEntity<Map> patient = post("/patients",
                Map.of("firstName", "Pat", "lastName", suffix,
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender", "MALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class);
        assertThat(patient.getStatusCode().is2xxSuccessful()).as("register patient").isTrue();
        return (String) patient.getBody().get("uid");
    }

    /** @return {@code [userUid, username]} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String[] createUser(String prefix, String role) {
        String username = prefix + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Map> created = post("/iam/users",
                Map.of("username", username, "password", "Passw0rd!123",
                        "firstName", "T", "lastName", prefix,
                        "email", username + "@test.local", "roles", Set.of(role)),
                Map.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).as("provision %s user", role).isTrue();
        return new String[]{(String) created.getBody().get("uid"), username};
    }
}
