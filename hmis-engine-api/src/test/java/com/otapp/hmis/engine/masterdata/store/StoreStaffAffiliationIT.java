package com.otapp.hmis.engine.masterdata.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Verifies the store keeper ⇄ store affiliation: assignment is gated on the
 * STORE_PERSON role, and issuing goods out of a store is refused unless the
 * acting user is affiliated with that store (legacy StorePerson.stores).
 */
class StoreStaffAffiliationIT extends AuthenticatedIntegrationTest {

    private static final String MAIN_STORE_UID    = "01J5KQRPCD0000000000000ST1";
    private static final String MAIN_PHARMACY_UID = "01J5KQRPCD0000000000000PH1";
    private static final String PANADOL_UID       = "01J5KQRPCD0000000000000MD1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assignListRemoveAndRoleGated() {
        String keeperUid = createUser("keeper", "STORE_PERSON");

        ResponseEntity<Map> assigned = post(staffPath(), Map.of("userUid", keeperUid), Map.class);
        assertThat(assigned.getStatusCode().value()).isEqualTo(201);
        assertThat(list()).anyMatch(s -> keeperUid.equals(s.get("userUid")));

        ResponseEntity<Void> removed = rest.exchange(
                staffPath() + "/uid/" + keeperUid, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);
        assertThat(removed.getStatusCode().value()).isEqualTo(204);
        assertThat(list()).noneMatch(s -> keeperUid.equals(s.get("userUid")));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assigningANonStoreKeeperIsRejected() {
        String nurseUid = createUser("nurse", "NURSE");
        ResponseEntity<Map> response = post(staffPath(), Map.of("userUid", nurseUid), Map.class);
        assertThat(response.getStatusCode().is4xxClientError())
                .as("A user without the STORE_PERSON role cannot be affiliated").isTrue();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void issuingFromAStoreRequiresMembership() {
        // Build the chain up to an approved TO (as ROOT — creation is not gated).
        expectOk(post("/store/stores/uid/" + MAIN_STORE_UID + "/stock/receive",
                Map.of("medicineUid", PANADOL_UID, "batchNo", "B-GATE-001",
                        "expiresAt", LocalDate.now().plusYears(2).toString(),
                        "quantity", 50, "note", "seed"), Map.class));
        Map ro = expectOk(post("/transfers/pharmacy-store/ro",
                Map.of("pharmacyUid", MAIN_PHARMACY_UID, "storeUid", MAIN_STORE_UID, "note", "x",
                        "lines", List.of(Map.of("medicineUid", PANADOL_UID, "quantity", 20))),
                Map.class));
        String roUid = (String) ro.get("uid");
        String roLineUid = (String) ((List<Map>) ro.get("lines")).get(0).get("uid");
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/verify", null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/approve", null, Map.class));
        expectOk(post("/transfers/pharmacy-store/ro/uid/" + roUid + "/submit", null, Map.class));
        Map to = expectOk(post("/transfers/pharmacy-store/to",
                Map.of("roUid", roUid, "lines",
                        List.of(Map.of("roLineUid", roLineUid, "quantity", 20))),
                Map.class));
        String toUid = (String) to.get("uid");
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/verify", null, Map.class));
        expectOk(post("/transfers/pharmacy-store/to/uid/" + toUid + "/approve", null, Map.class));

        // ROOT is not a store keeper affiliated with the store → issue refused.
        ResponseEntity<Map> denied = post(
                "/transfers/pharmacy-store/to/uid/" + toUid + "/issue", null, Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Issuing as a non-member must be refused").isTrue();
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        ResponseEntity<String> response = rest.getForEntity(staffPath(), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    // ----- helpers -----------------------------------------------------------

    private static String staffPath() {
        return "/masterdata/stores/uid/" + MAIN_STORE_UID + "/staff";
    }

    private List<Map<String, Object>> list() {
        ResponseEntity<List<Map<String, Object>>> response = get(staffPath(),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String createUser(String prefix, String role) {
        String username = prefix + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Map> created = post("/iam/users",
                Map.of("username", username, "password", "Passw0rd!123",
                        "firstName", "T", "lastName", prefix,
                        "email", username + "@test.local", "roles", Set.of(role)),
                Map.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).as("provision %s user", role).isTrue();
        return (String) created.getBody().get("uid");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        return response.getBody();
    }
}
