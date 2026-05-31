package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The admission deposit gate — process-audit cluster #2 (legacy Zana-HMIS
 * {@code doAdmission}: PENDING admission + WAITING bed until the ward-bed bill is
 * paid, then IN-PROCESS + OCCUPIED).
 *
 * <p>A CASH admission to a priced ward starts {@code AWAITING_DEPOSIT} with the bed
 * only RESERVED; settling the ward-bed bill activates it ({@code ADMITTED}) and
 * occupies the bed. An insurer-backed admission occupies immediately. Cancelling a
 * deposit-pending admission releases the reserved bed. A deposit-pending admission
 * still blocks a second admit.
 *
 * <p>Uses the Pediatric ward (priced here, off the General Ward that the charting
 * ITs admit to) and creates its own beds.
 */
class AdmissionDepositGateIT extends AuthenticatedIntegrationTest {

    private static final String PEDIATRIC_WARD_UID = "01J5KQRPCD0000000000000WG2";
    private static final String NHIF_PLAN_UID      = "01J5KQRPCD0000000000000IP1";
    private static final BigDecimal WARD_DAY       = new BigDecimal("15000.00");

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void cashAdmitReservesTheBedThenOccupiesItOnDepositPayment() {
        ensureWardPrice();
        String patientUid = registerPatient("CASH", null);
        String bedUid = createBed("DEP-A");

        Map<String, Object> admission = expectOk(post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", PEDIATRIC_WARD_UID, "bedUid", bedUid,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "obs"),
                Map.class));
        String admissionUid = (String) admission.get("uid");

        // Deposit-pending: admission AWAITING_DEPOSIT, bed only RESERVED (not occupied).
        assertThat(admission.get("status")).isEqualTo("AWAITING_DEPOSIT");
        assertThat(bedStatus(bedUid)).isEqualTo("RESERVED");

        // Settle the ward-bed bill → admission activates and the bed occupies.
        settleAdmissionBill(admissionUid);

        assertThat(expectOk(get("/encounters/admissions/uid/" + admissionUid, Map.class)).get("status"))
                .as("Deposit settled activates the admission").isEqualTo("ADMITTED");
        assertThat(bedStatus(bedUid))
                .as("Deposit settled occupies the reserved bed").isEqualTo("OCCUPIED");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void insuranceAdmitOccupiesTheBedImmediately() {
        ensureWardPrice();
        String patientUid = registerPatient("INSURANCE", NHIF_PLAN_UID);
        String bedUid = createBed("DEP-B");

        Map<String, Object> admission = expectOk(post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", PEDIATRIC_WARD_UID, "bedUid", bedUid,
                        "admittingClinicianUsername", "root", "paymentType", "INSURANCE",
                        "insurancePlanUid", NHIF_PLAN_UID, "admissionReason", "obs"),
                Map.class));

        // Insurer-backed admissions skip the deposit gate (legacy fully-covered branch).
        assertThat(admission.get("status")).isEqualTo("ADMITTED");
        assertThat(bedStatus(bedUid)).isEqualTo("OCCUPIED");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void cancellingADepositPendingAdmissionReleasesTheReservedBed() {
        ensureWardPrice();
        String patientUid = registerPatient("CASH", null);
        String bedUid = createBed("DEP-C");

        String admissionUid = (String) expectOk(post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", PEDIATRIC_WARD_UID, "bedUid", bedUid,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "obs"),
                Map.class)).get("uid");
        assertThat(bedStatus(bedUid)).isEqualTo("RESERVED");

        // A patient who never pays the deposit is cancelled — releasing the held bed.
        Map<String, Object> cancelled = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/cancel",
                Map.of("reason", "left before deposit"), Map.class));
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");
        assertThat(bedStatus(bedUid))
                .as("Cancelling a deposit-pending admission frees the reserved bed").isEqualTo("FREE");

        // The orphaned ward-bed invoice is voided after-commit — no live receivable.
        assertThat(expectOk(get("/billing/admissions/uid/" + admissionUid + "/invoice", Map.class)).get("status"))
                .as("Cancelling voids the unpaid ward-bed invoice").isEqualTo("CANCELLED");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mixedAdmitIsAlsoDepositGated() {
        ensureWardPrice();
        String patientUid = registerPatient("MIXED", NHIF_PLAN_UID);
        String bedUid = createBed("DEP-E");

        Map<String, Object> admission = expectOk(post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", PEDIATRIC_WARD_UID, "bedUid", bedUid,
                        "admittingClinicianUsername", "root", "paymentType", "MIXED",
                        "insurancePlanUid", NHIF_PLAN_UID, "admissionReason", "obs"),
                Map.class));

        // MIXED (insurance + cash top-up) owes cash, so it is deposit-gated like CASH.
        assertThat(admission.get("status")).isEqualTo("AWAITING_DEPOSIT");
        assertThat(bedStatus(bedUid)).isEqualTo("RESERVED");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void aDepositPendingAdmissionBlocksASecondAdmit() {
        ensureWardPrice();
        String patientUid = registerPatient("CASH", null);
        String bedUid = createBed("DEP-D");

        expectOk(post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", PEDIATRIC_WARD_UID, "bedUid", bedUid,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "obs"),
                Map.class));

        // Second admit while AWAITING_DEPOSIT is refused — the pending admission holds
        // the patient just like an active one (legacy PENDING blocks re-admit).
        ResponseEntity<Map> second = post("/encounters/admissions",
                Map.of("patientUid", patientUid, "wardUid", PEDIATRIC_WARD_UID,
                        "admittingClinicianUsername", "root", "paymentType", "CASH",
                        "admissionReason", "obs2"),
                Map.class);
        assertThat(second.getStatusCode().is4xxClientError())
                .as("A deposit-pending admission must block a second admit, got %s", second.getStatusCode())
                .isTrue();
    }

    // ----- fixtures ----------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private void ensureWardPrice() {
        ResponseEntity<Map> r = post("/masterdata/service-prices",
                Map.of("kind", "WARD", "serviceUid", PEDIATRIC_WARD_UID,
                        "amount", WARD_DAY, "currency", "TZS", "covered", false),
                Map.class);
        assertThat(r.getStatusCode().is2xxSuccessful() || r.getStatusCode().value() == 409)
                .as("WARD price seed expected 2xx or 409, got %s", r.getStatusCode()).isTrue();
    }

    @SuppressWarnings("rawtypes")
    private String registerPatient(String paymentType, String planUid) {
        var body = new java.util.HashMap<String, Object>();
        body.put("firstName", "Dep");
        body.put("lastName", "Gate");
        body.put("dateOfBirth", LocalDate.now().minusYears(20).toString());
        body.put("gender", "MALE");
        body.put("type", "OUTPATIENT");
        body.put("paymentType", paymentType);
        if (planUid != null) {
            body.put("insurancePlanUid", planUid);
            body.put("membershipNo", "DEP-" + Long.toString(System.nanoTime(), 36));
        }
        return (String) expectOk(post("/patients", body, Map.class)).get("uid");
    }

    @SuppressWarnings("rawtypes")
    private String createBed(String prefix) {
        String label = prefix + "-" + Long.toString(System.nanoTime(), 36);
        return (String) expectOk(post("/masterdata/wards/uid/" + PEDIATRIC_WARD_UID + "/beds",
                Map.of("label", label), Map.class)).get("uid");
    }

    @SuppressWarnings("rawtypes")
    private String bedStatus(String bedUid) {
        return (String) expectOk(get("/masterdata/beds/uid/" + bedUid, Map.class)).get("status");
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
