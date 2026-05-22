package com.otapp.hmis.engine.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * End-to-end test for the pharmacy↔store transfer chain:
 *   seed store stock → pharmacy RO → verify+approve+submit →
 *   store TO → verify+approve+issue (FEFO from store batches) →
 *   pharmacy RN → verify pharmacy stock landed.
 *
 * Uses seeded Main Pharmacy (PH1), Main Store (ST1) and Panadol (MD1)
 * from V4 — single-unit medicine so we don't have to wire conversion
 * coefficients into the test setup.
 */
class PharmacyStoreTransferIT extends AuthenticatedIntegrationTest {

    private static final String MAIN_STORE_UID    = "01J5KQRPCD0000000000000ST1";
    private static final String MAIN_PHARMACY_UID = "01J5KQRPCD0000000000000PH1";
    private static final String PANADOL_UID       = "01J5KQRPCD0000000000000MD1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void roundTripFromRoToRnLandsStockAtPharmacy() {
        // ----- seed store stock --------------------------------------------
        Map seeded = expectOk(post(
                "/store/stores/uid/" + MAIN_STORE_UID + "/stock/receive",
                Map.of(
                        "medicineUid", PANADOL_UID,
                        "batchNo",     "B-TEST-001",
                        "expiresAt",   LocalDate.now().plusYears(2).toString(),
                        "quantity",    100,
                        "note",        "Seed for transfer test"),
                Map.class));
        assertThat(((Number) seeded.get("quantity")).intValue()).isEqualTo(100);

        // ----- RO (pharmacy requests 60 from store) ------------------------
        Map ro = expectOk(post(
                "/transfers/pharmacy-store/ro",
                Map.of(
                        "pharmacyUid", MAIN_PHARMACY_UID,
                        "storeUid",    MAIN_STORE_UID,
                        "note",        "Restock OPD",
                        "lines", List.of(Map.of(
                                "medicineUid", PANADOL_UID,
                                "quantity",    60,
                                "note",        "Q1 restock"))),
                Map.class));
        String roUid = (String) ro.get("uid");
        String roLineUid = (String) ((List<Map>) ro.get("lines")).get(0).get("uid");
        assertThat(ro.get("status")).isEqualTo("PENDING");

        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/approve", null, Map.class));
        Map submittedRo = expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/submit",  null, Map.class));
        assertThat(submittedRo.get("status")).isEqualTo("SUBMITTED");

        // ----- TO (store ships 60) -----------------------------------------
        Map to = expectOk(post(
                "/transfers/pharmacy-store/to",
                Map.of(
                        "roUid", roUid,
                        "note",  "Picked from B-TEST-001",
                        "lines", List.of(Map.of(
                                "roLineUid", roLineUid,
                                "quantity",  60))),
                Map.class));
        String toUid = (String) to.get("uid");
        String toLineUid = (String) ((List<Map>) to.get("lines")).get(0).get("uid");
        assertThat(to.get("status")).isEqualTo("PENDING");

        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/approve", null, Map.class));

        // Issuing goods is gated on store membership (legacy StorePerson.stores):
        // provision a STORE_PERSON, affiliate them with the source store, and
        // issue the TO as that keeper.
        String keeperToken = issueAsAffiliatedKeeper(MAIN_STORE_UID);
        Map issuedTo = expectOk(postAs(keeperToken,
                "/transfers/pharmacy-store/to/uid/" + toUid + "/issue", null, Map.class));
        assertThat(issuedTo.get("status")).isEqualTo("GOODS_ISSUED");

        // TO line should now show 60 issued + at least one batch pick.
        Map issuedTOLine = ((List<Map>) issuedTo.get("lines")).get(0);
        assertThat(((Number) issuedTOLine.get("issuedQuantity")).intValue()).isEqualTo(60);
        List<Map> picks = (List<Map>) issuedTOLine.get("picks");
        assertThat(picks).isNotEmpty();
        int totalPicked = picks.stream().mapToInt(p -> ((Number) p.get("quantity")).intValue()).sum();
        assertThat(totalPicked).isEqualTo(60);

        // ----- RN (pharmacy confirms receipt) ------------------------------
        Map rn = expectOk(post(
                "/transfers/pharmacy-store/rn",
                Map.of(
                        "toUid",          toUid,
                        "receivingDate",  LocalDate.now().toString(),
                        "note",           "All units received intact",
                        "lines", List.of(Map.of(
                                "toLineUid",        toLineUid,
                                "receivedQuantity", 60))),
                Map.class));
        assertThat(rn.get("status")).isEqualTo("COMPLETED");

        // ----- pharmacy stock balance should now show 60 ------------------
        List<Map> pharmacyStock = expectOk(get(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID + "/stock", List.class));
        Map panadolBalance = pharmacyStock.stream()
                .filter(b -> PANADOL_UID.equals(b.get("medicineUid")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Panadol balance missing at pharmacy"));
        assertThat(((Number) panadolBalance.get("totalQuantity")).intValue()).isEqualTo(60);

        // RO should have rolled forward to COMPLETED as well.
        Map roAfter = expectOk(get("/transfers/pharmacy-store/ro/uid/" + roUid, Map.class));
        assertThat(roAfter.get("status")).isEqualTo("COMPLETED");
    }

    /** Provision a STORE_PERSON, affiliate them with the store, and return their access token. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String issueAsAffiliatedKeeper(String storeUid) {
        String username = "keeper" + Long.toString(System.nanoTime(), 36);
        Map created = expectOk(post("/iam/users",
                Map.of("username", username, "password", "Keeper!123",
                        "firstName", "Store", "lastName", "Keeper",
                        "email", username + "@test.local", "roles", Set.of("STORE_PERSON")),
                Map.class));
        String userUid = (String) created.get("uid");
        expectOk(post("/masterdata/stores/uid/" + storeUid + "/staff",
                Map.of("userUid", userUid), Map.class));
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(username, "Keeper!123"), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("Login failed for store keeper " + username);
        }
        return login.tokens().accessToken();
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
