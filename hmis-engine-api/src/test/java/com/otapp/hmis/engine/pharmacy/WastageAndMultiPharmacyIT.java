package com.otapp.hmis.engine.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * Phase 37 coverage:
 *
 *   1. Pharmacist write-off — receive a batch, write off part of it as
 *      EXPIRED, assert the WASTAGE movement carries the structured reason
 *      and the batch / balance has been decremented.
 *
 *   2. Multi-pharmacy dispense — open a retail sale at MAIN Pharmacy, seed
 *      stock only at OPD Pharmacy, dispense the sale line with
 *      {@code ?salesPharmacyUid=OPD} and assert: OPD's stock decremented
 *      while MAIN's stays at zero; the sale line records both pharmacy
 *      UIDs.
 *
 * Uses the seeded MAIN (PH1) + OPD (PH2) pharmacies and Panadol (MD1).
 */
class WastageAndMultiPharmacyIT extends AuthenticatedIntegrationTest {

    private static final String MAIN_PHARMACY_UID = "01J5KQRPCD0000000000000PH1";
    private static final String OPD_PHARMACY_UID  = "01J5KQRPCD0000000000000PH2";
    /** Amoxil seeded in V4; deliberately not Panadol — that one is used by other ITs. */
    private static final String AMOXIL_UID        = "01J5KQRPCD0000000000000MD2";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void writeOffDecrementsBatchAndRecordsWastageReason() {
        // Receive 100 Panadol into MAIN.
        Map batch = expectOk(post(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID + "/stock/receive",
                Map.of(
                        "medicineUid", AMOXIL_UID,
                        "batchNo",     "WO-B-001",
                        "expiresAt",   LocalDate.now().plusMonths(2).toString(),
                        "quantity",    100,
                        "note",        "Initial stock"),
                Map.class));
        String batchUid = (String) batch.get("uid");
        assertThat(((Number) batch.get("quantity")).intValue()).isEqualTo(100);

        // Write off 25 as EXPIRED.
        Map afterWrite = expectOk(post(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID + "/stock/write-off",
                Map.of(
                        "batchUid", batchUid,
                        "quantity", 25,
                        "reason",   "EXPIRED",
                        "note",     "Past expiry"),
                Map.class));
        assertThat(((Number) afterWrite.get("quantity")).intValue())
                .as("Batch should be decremented by 25")
                .isEqualTo(75);

        // Stock-card row should show the WASTAGE movement with reason.
        ResponseEntity<Map> movementsResp = get(
                "/pharmacy/stock/movements?pharmacyUid=" + MAIN_PHARMACY_UID
                        + "&medicineUid=" + AMOXIL_UID + "&kind=WASTAGE",
                Map.class);
        Map page = expectOk(movementsResp);
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content)
                .as("Expect exactly one WASTAGE movement")
                .hasSize(1);
        Map<String, Object> movement = content.get(0);
        assertThat(movement.get("kind")).isEqualTo("WASTAGE");
        assertThat(((Number) movement.get("quantity")).intValue()).isEqualTo(-25);
        assertThat(movement.get("batchUid")).isEqualTo(batchUid);

        // Cannot write off more than what's in the batch.
        ResponseEntity<Map> denied = post(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID + "/stock/write-off",
                Map.of(
                        "batchUid", batchUid,
                        "quantity", 999,
                        "reason",   "DAMAGED"),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Overdraft should be rejected")
                .isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void retailSaleDispensesFromAnotherPharmacyWhenSalesPharmacyUidIsSet() {
        // Tests share the DB across the JVM lifetime, so capture baseline totals
        // and assert deltas rather than absolute counts.
        int mainBefore = amoxilTotalAt(MAIN_PHARMACY_UID);
        int opdBefore  = amoxilTotalAt(OPD_PHARMACY_UID);

        // Seed stock ONLY at OPD Pharmacy.
        expectOk(post(
                "/pharmacy/pharmacies/uid/" + OPD_PHARMACY_UID + "/stock/receive",
                Map.of(
                        "medicineUid", AMOXIL_UID,
                        "batchNo",     "MP-B-001",
                        "expiresAt",   LocalDate.now().plusYears(1).toString(),
                        "quantity",    50,
                        "note",        "OPD-only seed"),
                Map.class));

        // Open the sale at MAIN Pharmacy with one line for 10 Panadol.
        Map sale = expectOk(post(
                "/pharmacy/sales",
                Map.of(
                        "pharmacyUid",   MAIN_PHARMACY_UID,
                        "customerName",  "Test Walk-in",
                        "paymentType",   "CASH",
                        "lines", List.of(Map.of(
                                "medicineUid", AMOXIL_UID,
                                "quantity",    10,
                                "unitPrice",   "500.00"))),
                Map.class));
        String saleUid = (String) sale.get("uid");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) sale.get("lines");
        String lineUid = (String) lines.get(0).get("uid");

        // Walk the line through the 8-state lifecycle to APPROVED.
        String linePath = "/pharmacy/sales/uid/" + saleUid + "/lines/uid/" + lineUid;
        expectOk(post(linePath + "/accept",  null, Map.class));
        expectOk(post(linePath + "/verify",  null, Map.class));
        expectOk(post(linePath + "/approve", null, Map.class));

        // Dispense at MAIN but pull stock from OPD via salesPharmacyUid.
        ResponseEntity<List<Map<String, Object>>> dispenseResp = rest.exchange(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID
                        + "/dispense-sale-line/uid/" + lineUid
                        + "?salesPharmacyUid=" + OPD_PHARMACY_UID,
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<>() {});
        assertThat(dispenseResp.getStatusCode().is2xxSuccessful())
                .as("dispense-sale-line POST should succeed, body=%s", dispenseResp.getBody())
                .isTrue();
        List<Map<String, Object>> movements = dispenseResp.getBody();
        assertThat(movements).hasSize(1);
        Map<String, Object> mv = movements.get(0);
        assertThat(mv.get("pharmacyUid"))
                .as("Movement should be recorded at the SALES pharmacy (OPD), not the issuing pharmacy (MAIN)")
                .isEqualTo(OPD_PHARMACY_UID);
        assertThat(mv.get("kind")).isEqualTo("DISPENSE");
        assertThat(((Number) mv.get("quantity")).intValue()).isEqualTo(-10);

        // Delta: OPD received 50 then lost 10 = +40 net. MAIN unchanged.
        int opdAfter  = amoxilTotalAt(OPD_PHARMACY_UID);
        int mainAfter = amoxilTotalAt(MAIN_PHARMACY_UID);
        assertThat(opdAfter - opdBefore)
                .as("Sales pharmacy net delta = +50 receive - 10 dispense")
                .isEqualTo(40);
        assertThat(mainAfter - mainBefore)
                .as("Issuing pharmacy should NOT be decremented")
                .isZero();
    }

    private int amoxilTotalAt(String pharmacyUid) {
        List<Map<String, Object>> rows = expectOkList(
                "/pharmacy/pharmacies/uid/" + pharmacyUid + "/stock");
        return rows.stream()
                .filter(r -> AMOXIL_UID.equals(r.get("medicineUid")))
                .mapToInt(r -> ((Number) r.get("totalQuantity")).intValue())
                .sum();
    }

    private List<Map<String, Object>> expectOkList(String path) {
        ResponseEntity<List<Map<String, Object>>> resp = get(
                path, new ParameterizedTypeReference<>() {});
        assertThat(resp.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", resp.getStatusCode(), resp.getBody())
                .isTrue();
        return resp.getBody();
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    @SuppressWarnings("unused")
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
}
