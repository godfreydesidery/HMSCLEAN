package com.otapp.hmis.engine.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * Phase 43 — verify the manual stock paths and GRN ingestion convert
 * user-typed quantities through {@code unitUid} into base units.
 *
 * Uses Coartem (MD3) so we don't collide with the existing Panadol +
 * Amoxil ITs. A "PACK10" unit with factor 10 is created at test-time.
 */
class UnitAwareStockIT extends AuthenticatedIntegrationTest {

    private static final String COARTEM_UID       = "01J5KQRPCD0000000000000MD3";
    private static final String MAIN_PHARMACY_UID = "01J5KQRPCD0000000000000PH1";

    @Test
    @SuppressWarnings("rawtypes")
    void receiveWithUnitUidConvertsToBase() {
        // 1. Create a PACK10 alternate unit on Coartem (factor 10). Code max
        //    is 16 chars; suffix with the low 6 digits of nanoTime to dedup
        //    across reruns within the JVM-lifetime DB.
        String suffix = String.valueOf(System.nanoTime() % 1_000_000L);
        Map unit = expectOk(post(
                "/masterdata/medicines/uid/" + COARTEM_UID + "/units",
                Map.of("code", "PK10_" + suffix,
                        "name", "Pack of 10 tablets",
                        "factorToBase", 10),
                Map.class));
        String unitUid = (String) unit.get("uid");
        assertThat(((Number) unit.get("factorToBase")).intValue()).isEqualTo(10);

        // 2. Baseline stock at MAIN.
        int before = coartemTotalAt(MAIN_PHARMACY_UID);

        // 3. Receive 5 packs — expect 50 tablets added to base balance.
        Map received = expectOk(post(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID + "/stock/receive",
                Map.of(
                        "medicineUid", COARTEM_UID,
                        "batchNo",     "UNIT-B-" + System.nanoTime(),
                        "expiresAt",   LocalDate.now().plusMonths(8).toString(),
                        "quantity",    5,
                        "unitUid",     unitUid,
                        "note",        "5 packs = 50 tablets"),
                Map.class));
        assertThat(((Number) received.get("quantity")).intValue())
                .as("Batch stores base-unit count (50), not the typed 5")
                .isEqualTo(50);

        int after = coartemTotalAt(MAIN_PHARMACY_UID);
        assertThat(after - before)
                .as("Pharmacy balance increased by 50 base units")
                .isEqualTo(50);
    }

    private int coartemTotalAt(String pharmacyUid) {
        ResponseEntity<List<Map<String, Object>>> resp = get(
                "/pharmacy/pharmacies/uid/" + pharmacyUid + "/stock",
                new ParameterizedTypeReference<>() {});
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> body = resp.getBody();
        if (body == null) return 0;
        return body.stream()
                .filter(r -> COARTEM_UID.equals(r.get("medicineUid")))
                .mapToInt(r -> ((Number) r.get("totalQuantity")).intValue())
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
