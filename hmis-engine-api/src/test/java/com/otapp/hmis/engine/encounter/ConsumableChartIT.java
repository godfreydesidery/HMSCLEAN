package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * Phase 41 — patient consumable chart end-to-end:
 *
 *   Register OUTPATIENT → admit to General Ward → issue gauze and saline
 *   against the admission → assert chart has both rows → regenerate the
 *   admission invoice → assert CONSUMABLE lines were picked up with the
 *   snapshot unit costs.
 *
 * Uses the seeded General Ward (V4) + GAUZE / NSALINE consumables (V43).
 */
class ConsumableChartIT extends AuthenticatedIntegrationTest {

    private static final String GENERAL_WARD_UID = "01J5KQRPCD0000000000000WG1";
    private static final String GAUZE_UID        = "01J5KQRPCD0000000000000CB1";
    private static final String SALINE_UID       = "01J5KQRPCD0000000000000CB2";
    private static final String MAIN_PHARMACY    = "01J5KQRPCD0000000000000PH1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void issuesFlowThroughChartAndIntoAdmissionInvoice() {
        // 1. Register the patient (CASH; pay the registration fee so booking
        //    isn't gated — admission doesn't currently consult the gate but
        //    cleaner to mirror real flow).
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Cons",
                        "lastName",    "Umable",
                        "dateOfBirth", LocalDate.now().minusYears(50).toString(),
                        "gender",      "FEMALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)).get("uid");

        // 2. Admit to General Ward as root.
        Map admission = expectOk(post(
                "/encounters/admissions",
                Map.of(
                        "patientUid",                  patientUid,
                        "wardUid",                     GENERAL_WARD_UID,
                        "admittingClinicianUsername",  "root",
                        "paymentType",                 "CASH",
                        "admissionReason",             "observation"),
                Map.class));
        String admissionUid = (String) admission.get("uid");
        assertThat(admission.get("status")).isEqualTo("ADMITTED");

        // 3a. Seed source stock at MAIN_PHARMACY so the issue decrements
        //     successfully (Phase 46: issues now require on-hand stock).
        expectOk(post("/consumables/stock/receive",
                Map.of(
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "consumableUid",     GAUZE_UID,
                        "quantity",          20),
                Map.class));
        expectOk(post("/consumables/stock/receive",
                Map.of(
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "consumableUid",     SALINE_UID,
                        "quantity",          10),
                Map.class));

        // 3. Issue two consumables on the chart.
        Map gauzeIssue = expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/consumables",
                Map.of(
                        "consumableUid",     GAUZE_UID,
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "quantity",          3,
                        "unitCost",          "500.00",
                        "note",              "wound care"),
                Map.class));
        assertThat(gauzeIssue.get("consumableCode")).isEqualTo("GAUZE");
        assertThat(new BigDecimal(gauzeIssue.get("lineAmount").toString()))
                .isEqualByComparingTo("1500.00");

        expectOk(post(
                "/encounters/admissions/uid/" + admissionUid + "/consumables",
                Map.of(
                        "consumableUid",     SALINE_UID,
                        "sourceKind",        "PHARMACY",
                        "sourceLocationUid", MAIN_PHARMACY,
                        "quantity",          2,
                        "unitCost",          "2500.00"),
                Map.class));

        // 4. Chart shows both rows in issued-at order.
        ResponseEntity<List<Map<String, Object>>> chart = get(
                "/encounters/admissions/uid/" + admissionUid + "/consumables",
                new ParameterizedTypeReference<>() {});
        assertThat(chart.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(chart.getBody()).hasSize(2);

        // 5. The admission invoice generator picks them up as CONSUMABLE lines.
        Map invoice = expectOk(post(
                "/billing/admissions/uid/" + admissionUid + "/invoice",
                null,
                Map.class));
        List<Map<String, Object>> lines = (List<Map<String, Object>>) invoice.get("lines");
        long consumableLines = lines.stream()
                .filter(l -> "CONSUMABLE".equals(l.get("kind")))
                .count();
        assertThat(consumableLines)
                .as("Admission invoice should have one CONSUMABLE line per issue")
                .isEqualTo(2);

        BigDecimal consumableTotal = lines.stream()
                .filter(l -> "CONSUMABLE".equals(l.get("kind")))
                .map(l -> new BigDecimal(l.get("amount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(consumableTotal)
                .as("3×500 + 2×2500")
                .isEqualByComparingTo("6500.00");
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
