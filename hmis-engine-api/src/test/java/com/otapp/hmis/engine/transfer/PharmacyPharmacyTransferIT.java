package com.otapp.hmis.engine.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test for the pharmacy↔pharmacy transfer chain, focused on the
 * G1 short-receive integrity fix: when the requesting pharmacy receives fewer
 * units than the delivering pharmacy issued, the full issued quantity lands at
 * the requesting pharmacy and the shortfall is written off there as a
 * WASTAGE/LOST transit loss — so inventory never silently disappears.
 *
 * Uses seeded Main Pharmacy (PH1, deliverer), OPD Pharmacy (PH2, requester)
 * and Panadol (MD1) from V4 — single-unit medicine so no conversion wiring.
 */
class PharmacyPharmacyTransferIT extends AuthenticatedIntegrationTest {

    private static final String DELIVERING_PHARMACY_UID = "01J5KQRPCD0000000000000PH1";
    private static final String REQUESTING_PHARMACY_UID = "01J5KQRPCD0000000000000PH2";
    private static final String PANADOL_UID             = "01J5KQRPCD0000000000000MD1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shortReceiveWritesOffShortfallAsTransitLoss() {
        // ----- seed delivering pharmacy stock ------------------------------
        expectOk(post(
                "/pharmacy/pharmacies/uid/" + DELIVERING_PHARMACY_UID + "/stock/receive",
                Map.of(
                        "medicineUid", PANADOL_UID,
                        "batchNo",     "P2P-SHORT-001",
                        "expiresAt",   LocalDate.now().plusYears(2).toString(),
                        "quantity",    40,
                        "note",        "Seed for P2P short-receive test"),
                Map.class));

        // ----- RO (PH2 requests 30 from PH1) -------------------------------
        Map ro = expectOk(post(
                "/transfers/pharmacy-pharmacy/ro",
                Map.of(
                        "requestingPharmacyUid", REQUESTING_PHARMACY_UID,
                        "deliveringPharmacyUid", DELIVERING_PHARMACY_UID,
                        "lines", List.of(Map.of("medicineUid", PANADOL_UID, "quantity", 30))),
                Map.class));
        String roUid = (String) ro.get("uid");
        String roLineUid = (String) ((List<Map>) ro.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-pharmacy/ro/uid/" + roUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-pharmacy/ro/uid/" + roUid + "/approve", null, Map.class));
        expectOk(post("/transfers/pharmacy-pharmacy/ro/uid/" + roUid + "/submit",  null, Map.class));

        // ----- TO (PH1 ships 30) -------------------------------------------
        Map to = expectOk(post(
                "/transfers/pharmacy-pharmacy/to",
                Map.of("roUid", roUid,
                        "lines", List.of(Map.of("roLineUid", roLineUid, "quantity", 30))),
                Map.class));
        String toUid = (String) to.get("uid");
        String toLineUid = (String) ((List<Map>) to.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-pharmacy/to/uid/" + toUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-pharmacy/to/uid/" + toUid + "/approve", null, Map.class));
        // P↔P issue is intentionally NOT gated on any affiliation model.
        Map issued = expectOk(post(
                "/transfers/pharmacy-pharmacy/to/uid/" + toUid + "/issue", null, Map.class));
        assertThat(issued.get("status")).isEqualTo("GOODS_ISSUED");

        int before = pharmacyBalance(REQUESTING_PHARMACY_UID, PANADOL_UID);

        // ----- RN (PH2 receives only 25 of 30) -----------------------------
        Map rn = expectOk(post(
                "/transfers/pharmacy-pharmacy/rn",
                Map.of("toUid", toUid, "receivingDate", LocalDate.now().toString(),
                        "lines", List.of(Map.of("toLineUid", toLineUid, "receivedQuantity", 25))),
                Map.class));
        assertThat(rn.get("status")).isEqualTo("COMPLETED");

        // Requesting pharmacy credited by exactly 25 (issued 30 in, 5 lost).
        int after = pharmacyBalance(REQUESTING_PHARMACY_UID, PANADOL_UID);
        assertThat(after - before).isEqualTo(25);

        // A WASTAGE/LOST transit-loss movement of 5 must exist at PH2.
        Map wastagePage = expectOk(get(
                "/pharmacy/stock/movements?pharmacyUid=" + REQUESTING_PHARMACY_UID
                        + "&medicineUid=" + PANADOL_UID + "&kind=WASTAGE&size=200", Map.class));
        List<Map> wastages = (List<Map>) wastagePage.get("content");
        boolean lostFive = wastages.stream().anyMatch(m ->
                "WASTAGE".equals(m.get("kind"))
                        && ((Number) m.get("quantity")).intValue() == -5
                        && ((String) m.get("note")).contains("Transit loss"));
        assertThat(lostFive)
                .as("Expected a WASTAGE/LOST transit-loss movement of 5 units at PH2, got %s", wastages)
                .isTrue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int pharmacyBalance(String pharmacyUid, String medicineUid) {
        Map page = expectOk(get(
                "/pharmacy/pharmacies/uid/" + pharmacyUid + "/stock?size=200", Map.class));
        List<Map> content = (List<Map>) page.get("content");
        return content.stream()
                .filter(b -> medicineUid.equals(b.get("medicineUid")))
                .findFirst()
                .map(b -> ((Number) b.get("totalQuantity")).intValue())
                .orElse(0);
    }

    private static <T> T expectOk(org.springframework.http.ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
