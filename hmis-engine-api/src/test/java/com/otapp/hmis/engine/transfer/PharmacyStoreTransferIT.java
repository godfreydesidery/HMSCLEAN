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
        // Shared JVM-lifetime container: assert the +60 DELTA, not an absolute
        // balance (other tests in the suite also move MD1 into this pharmacy).
        int beforePharmacy = pharmacyBalance(MAIN_PHARMACY_UID, PANADOL_UID);

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

        // ----- pharmacy stock should have grown by exactly 60 -------------
        int afterPharmacy = pharmacyBalance(MAIN_PHARMACY_UID, PANADOL_UID);
        assertThat(afterPharmacy - beforePharmacy).isEqualTo(60);

        // RO should have rolled forward to COMPLETED as well.
        Map roAfter = expectOk(get("/transfers/pharmacy-store/ro/uid/" + roUid, Map.class));
        assertThat(roAfter.get("status")).isEqualTo("COMPLETED");
    }

    /**
     * G1: a short-receive (RN receivedQuantity < TO issuedQuantity) must not
     * silently leak stock. The full issued quantity lands at the pharmacy and
     * the shortfall is written off there as a WASTAGE/LOST transit loss, so
     * the net pharmacy balance equals exactly what was received.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shortReceiveWritesOffShortfallAsTransitLoss() {
        // Seed store stock to transfer.
        expectOk(post(
                "/store/stores/uid/" + MAIN_STORE_UID + "/stock/receive",
                Map.of(
                        "medicineUid", PANADOL_UID,
                        "batchNo",     "B-SHORT-001",
                        "expiresAt",   LocalDate.now().plusYears(2).toString(),
                        "quantity",    50,
                        "note",        "Seed for short-receive test"),
                Map.class));

        // RO 30 → TO 30 → issue 30.
        Map ro = expectOk(post(
                "/transfers/pharmacy-store/ro",
                Map.of("pharmacyUid", MAIN_PHARMACY_UID, "storeUid", MAIN_STORE_UID,
                        "lines", List.of(Map.of("medicineUid", PANADOL_UID, "quantity", 30))),
                Map.class));
        String roUid = (String) ro.get("uid");
        String roLineUid = (String) ((List<Map>) ro.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/approve", null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/submit",  null, Map.class));

        Map to = expectOk(post(
                "/transfers/pharmacy-store/to",
                Map.of("roUid", roUid,
                        "lines", List.of(Map.of("roLineUid", roLineUid, "quantity", 30))),
                Map.class));
        String toUid = (String) to.get("uid");
        String toLineUid = (String) ((List<Map>) to.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/approve", null, Map.class));

        String keeperToken = issueAsAffiliatedKeeper(MAIN_STORE_UID);
        expectOk(postAs(keeperToken, "/transfers/pharmacy-store/to/uid/" + toUid + "/issue", null, Map.class));

        // Pharmacy balance before this RN, so we can assert the +25 delta.
        int before = pharmacyBalance(MAIN_PHARMACY_UID, PANADOL_UID);

        // RN receives only 25 of the 30 issued.
        Map rn = expectOk(post(
                "/transfers/pharmacy-store/rn",
                Map.of("toUid", toUid, "receivingDate", LocalDate.now().toString(),
                        "lines", List.of(Map.of("toLineUid", toLineUid, "receivedQuantity", 25))),
                Map.class));
        assertThat(rn.get("status")).isEqualTo("COMPLETED");

        // Pharmacy balance credited by exactly 25 (issued 30 in, 5 written off).
        int after = pharmacyBalance(MAIN_PHARMACY_UID, PANADOL_UID);
        assertThat(after - before).isEqualTo(25);

        // A WASTAGE/LOST movement of quantity -5 must exist for this medicine.
        Map wastagePage = expectOk(get(
                "/pharmacy/stock/movements?pharmacyUid=" + MAIN_PHARMACY_UID
                        + "&medicineUid=" + PANADOL_UID + "&kind=WASTAGE&size=200", Map.class));
        List<Map> wastages = (List<Map>) wastagePage.get("content");
        boolean lostFive = wastages.stream().anyMatch(m ->
                "WASTAGE".equals(m.get("kind"))
                        && ((Number) m.get("quantity")).intValue() == -5
                        && m.get("referenceUid") != null
                        && ((String) m.get("note")).contains("Transit loss"));
        assertThat(lostFive)
                .as("Expected a WASTAGE/LOST transit-loss movement of 5 units, got %s", wastages)
                .isTrue();

    }

    /**
     * G3: a store→pharmacy transfer must carry the source batch's manufactured
     * date onto the destination pharmacy batch. Uses a freshly-created medicine
     * so the store holds exactly one batch of it — FEFO is then deterministic
     * regardless of what other tests left in the shared container.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void transferCarriesManufacturedDateToPharmacyBatch() {
        String code = "TXG3" + Long.toString(System.nanoTime(), 36).toUpperCase();
        Map med = expectOk(post("/masterdata/medicines",
                Map.of("code", code, "name", "Transfer G3 " + code, "form", "TABLET"),
                Map.class));
        String medUid = (String) med.get("uid");
        LocalDate mfg = LocalDate.now().minusMonths(4);

        expectOk(post("/store/stores/uid/" + MAIN_STORE_UID + "/stock/receive",
                Map.of("medicineUid", medUid, "batchNo", "G3-001",
                        "expiresAt", LocalDate.now().plusYears(2).toString(),
                        "quantity", 40, "note", "G3 seed"),
                Map.class));
        // The plain store receive endpoint has no mfg field — stamp it directly.
        stampStoreBatchManufacturedDate(medUid, "G3-001", mfg);

        Map ro = expectOk(post("/transfers/pharmacy-store/ro",
                Map.of("pharmacyUid", MAIN_PHARMACY_UID, "storeUid", MAIN_STORE_UID,
                        "lines", List.of(Map.of("medicineUid", medUid, "quantity", 20))),
                Map.class));
        String roUid = (String) ro.get("uid");
        String roLineUid = (String) ((List<Map>) ro.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/approve", null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/submit",  null, Map.class));

        Map to = expectOk(post("/transfers/pharmacy-store/to",
                Map.of("roUid", roUid, "lines", List.of(Map.of("roLineUid", roLineUid, "quantity", 20))),
                Map.class));
        String toUid = (String) to.get("uid");
        String toLineUid = (String) ((List<Map>) to.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/verify",  null, Map.class));
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/approve", null, Map.class));
        String keeperToken = issueAsAffiliatedKeeper(MAIN_STORE_UID);
        expectOk(postAs(keeperToken, "/transfers/pharmacy-store/to/uid/" + toUid + "/issue", null, Map.class));

        expectOk(post("/transfers/pharmacy-store/rn",
                Map.of("toUid", toUid, "receivingDate", LocalDate.now().toString(),
                        "lines", List.of(Map.of("toLineUid", toLineUid, "receivedQuantity", 20))),
                Map.class));

        Map batch = pharmacyBatch(MAIN_PHARMACY_UID, medUid, "G3-001");
        assertThat(batch).as("pharmacy batch G3-001 should exist").isNotNull();
        assertThat(batch.get("manufacturedDate")).isEqualTo(mfg.toString());
    }

    /** Sum of the pharmacy's on-hand quantity for one medicine. */
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

    /** The pharmacy's batch DTO for one (medicine, batchNo), or null. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map pharmacyBatch(String pharmacyUid, String medicineUid, String batchNo) {
        Map page = expectOk(get(
                "/pharmacy/pharmacies/uid/" + pharmacyUid + "/stock?size=200", Map.class));
        List<Map> content = (List<Map>) page.get("content");
        return content.stream()
                .filter(b -> medicineUid.equals(b.get("medicineUid")))
                .flatMap(b -> ((List<Map>) b.get("batchDetails")).stream())
                .filter(d -> batchNo.equals(d.get("batchNo")))
                .findFirst()
                .orElse(null);
    }

    /**
     * The plain store receive endpoint has no manufactured-date field (only
     * the procurement goods-receipt path is mfg-aware), so stamp the seeded
     * store batch directly via the repository for this transfer test.
     * {@code saveAndFlush} commits in its own transaction, so the date is
     * visible to the subsequent HTTP-driven transfer.
     */
    private void stampStoreBatchManufacturedDate(String medicineUid, String batchNo, LocalDate mfg) {
        com.otapp.hmis.engine.store.stock.domain.StoreStockBatch batch =
                storeBatchRepository.findByStoreUidAndMedicineUidAndBatchNo(
                                MAIN_STORE_UID, medicineUid, batchNo)
                        .orElseThrow(() -> new AssertionError("seeded store batch missing: " + batchNo));
        batch.setManufacturedDate(mfg);
        storeBatchRepository.saveAndFlush(batch);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.otapp.hmis.engine.store.stock.domain.StoreStockBatchRepository storeBatchRepository;

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
