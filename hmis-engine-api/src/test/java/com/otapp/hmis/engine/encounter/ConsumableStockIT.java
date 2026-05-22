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
 * Phase 46 — consumable stock decrement at source. Receive 15 saline at
 * MAIN_PHARMACY, issue 10 against an admission, assert balance drops to
 * 5, then assert an over-quota issue is refused and the balance is
 * unchanged.
 */
class ConsumableStockIT extends AuthenticatedIntegrationTest {

    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";
    private static final String SALINE_UID       = "01J5KQRPCD0000000000000CB2";
    private static final String MAIN_PHARMACY    = "01J5KQRPCD0000000000000PH1";

    @Test
    @SuppressWarnings("rawtypes")
    void issueDecrementsAndOverdraftIsRefused() {
        int before = balance();

        // Receive 15 units.
        expectOk(post("/consumables/stock/receive",
                Map.of(
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "consumableUid",     SALINE_UID,
                        "quantity",          15,
                        "note",              "Phase 46 seed"),
                Map.class));
        assertThat(balance() - before).isEqualTo(15);

        // Admit a patient so we have a target for the issue.
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Stock",
                        "lastName",    "Drain",
                        "dateOfBirth", LocalDate.now().minusYears(40).toString(),
                        "gender",      "MALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)).get("uid");
        String admissionUid = (String) expectOk(post(
                "/encounters/admissions",
                Map.of(
                        "patientUid",                  patientUid,
                        "wardUid",                     GENERAL_WARD_UID,
                        "admittingClinicianUsername",  "root",
                        "paymentType",                 "CASH",
                        "admissionReason",             "observation"),
                Map.class)).get("uid");

        // Issue 10 — should decrement to before+5.
        expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/consumables",
                Map.of(
                        "consumableUid",     SALINE_UID,
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "quantity",          10,
                        "unitCost",          "2500.00"),
                Map.class));
        assertThat(balance() - before)
                .as("Receipt 15 - issue 10 = +5 vs baseline")
                .isEqualTo(5);

        // Overdraft (request one more than what's on hand) — refused, balance unchanged.
        // We use the live balance because other ITs share the JVM-lifetime DB and
        // may have left non-zero saline stock at MAIN before this test ran.
        int remaining = balance();
        ResponseEntity<Map> overdraft = post(
                "/encounters/admissions/uid/" + admissionUid + "/consumables",
                Map.of(
                        "consumableUid",     SALINE_UID,
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "quantity",          remaining + 1,
                        "unitCost",          "2500.00"),
                Map.class);
        assertThat(overdraft.getStatusCode().is4xxClientError())
                .as("Overdraft (req %s of %s on hand) should be refused", remaining + 1, remaining)
                .isTrue();
        assertThat(balance())
                .as("Failed issue must not move the balance")
                .isEqualTo(remaining);
    }

    private int balance() {
        ResponseEntity<List<Map<String, Object>>> resp = get(
                "/consumables/stock/by-source?sourceKind=PHARMACY&sourceUid=" + MAIN_PHARMACY,
                new ParameterizedTypeReference<>() {});
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> body = resp.getBody();
        if (body == null) return 0;
        return body.stream()
                .filter(r -> SALINE_UID.equals(r.get("consumableUid")))
                .mapToInt(r -> ((Number) r.get("quantity")).intValue())
                .sum();
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
