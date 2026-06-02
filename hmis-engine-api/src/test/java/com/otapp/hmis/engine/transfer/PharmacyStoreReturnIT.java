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
 * Pharmacy→store return chain, focused on the G2 affiliation gate: completing
 * a return moves stock into a store, so it must be gated on store membership
 * of the return's destination store (legacy {@code StorePerson.stores}) — the
 * coarse PHARMACY_ACCESS/STORE_ACCESS authority alone isn't enough.
 *
 * Uses seeded Main Pharmacy (PH1), Main Store (ST1) and Panadol (MD1) from V4.
 */
class PharmacyStoreReturnIT extends AuthenticatedIntegrationTest {

    private static final String MAIN_STORE_UID    = "01J5KQRPCD0000000000000ST1";
    private static final String MAIN_PHARMACY_UID = "01J5KQRPCD0000000000000PH1";
    private static final String PANADOL_UID       = "01J5KQRPCD0000000000000MD1";

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void completeIsGatedOnDestinationStoreMembership() {
        // ----- seed pharmacy stock to return -------------------------------
        expectOk(post(
                "/pharmacy/pharmacies/uid/" + MAIN_PHARMACY_UID + "/stock/receive",
                Map.of(
                        "medicineUid", PANADOL_UID,
                        "batchNo",     "RET-B-001",
                        "expiresAt",   LocalDate.now().plusYears(2).toString(),
                        "quantity",    40,
                        "note",        "Seed for return test"),
                Map.class));

        // ----- create + submit return (20 back to store) -------------------
        Map ret = expectOk(post(
                "/transfers/pharmacy-store/returns",
                Map.of(
                        "pharmacyUid", MAIN_PHARMACY_UID,
                        "storeUid",    MAIN_STORE_UID,
                        "returnDate",  LocalDate.now().toString(),
                        "reason",      "Overstock",
                        "lines", List.of(Map.of("medicineUid", PANADOL_UID, "quantity", 20))),
                Map.class));
        String returnUid = (String) ret.get("uid");
        assertThat(ret.get("status")).isEqualTo("DRAFT");

        Map submitted = expectOk(post(
                "/transfers/pharmacy-store/returns/uid/" + returnUid + "/submit", null, Map.class));
        assertThat(submitted.get("status")).isEqualTo("SUBMITTED");

        int storeBefore = storeBalance(MAIN_STORE_UID, PANADOL_UID);

        // ----- complete as ROOT (unaffiliated) is rejected 422 -------------
        org.springframework.http.ResponseEntity<Map> rejected = post(
                "/transfers/pharmacy-store/returns/uid/" + returnUid + "/complete", null, Map.class);
        assertThat(rejected.getStatusCode().value()).isEqualTo(422);
        assertThat(rejected.getBody()).isNotNull();
        assertThat(rejected.getBody().get("code")).isEqualTo("BUSINESS_RULE");

        // Store untouched, return still SUBMITTED.
        assertThat(storeBalance(MAIN_STORE_UID, PANADOL_UID)).isEqualTo(storeBefore);
        Map stillSubmitted = expectOk(get(
                "/transfers/pharmacy-store/returns/uid/" + returnUid, Map.class));
        assertThat(stillSubmitted.get("status")).isEqualTo("SUBMITTED");

        // ----- complete as a store-affiliated keeper succeeds --------------
        String keeperToken = affiliatedStoreKeeper(MAIN_STORE_UID);
        Map completed = expectOk(postAs(keeperToken,
                "/transfers/pharmacy-store/returns/uid/" + returnUid + "/complete", null, Map.class));
        assertThat(completed.get("status")).isEqualTo("COMPLETED");

        // Store credited by 20.
        assertThat(storeBalance(MAIN_STORE_UID, PANADOL_UID) - storeBefore).isEqualTo(20);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int storeBalance(String storeUid, String medicineUid) {
        Map page = expectOk(get(
                "/store/stores/uid/" + storeUid + "/stock?size=200", Map.class));
        List<Map> content = (List<Map>) page.get("content");
        return content.stream()
                .filter(b -> medicineUid.equals(b.get("medicineUid")))
                .findFirst()
                .map(b -> ((Number) b.get("totalQuantity")).intValue())
                .orElse(0);
    }

    /** Provision a STORE_PERSON, affiliate them with the store, return their token. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String affiliatedStoreKeeper(String storeUid) {
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
