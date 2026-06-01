package com.otapp.hmis.engine.masterdata.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Verifies the storekeeper "my stores" picker: GET /masterdata/stores/mine
 * returns exactly the active stores the calling user is affiliated with via
 * {@code StoreStaff} (legacy {@code load_stores_by_store_person}), and no
 * unaffiliated store.
 */
class MyStoresIT extends AuthenticatedIntegrationTest {

    private static final String MAIN_STORE_UID = "01J5KQRPCD0000000000000ST1";
    private static final String CONS_STORE_UID = "01J5KQRPCD0000000000000ST2";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mineReturnsOnlyAffiliatedStores() {
        // Provision a storekeeper and affiliate them with ONE store (as ROOT).
        String password = "Passw0rd!123";
        String username = "keeper" + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Map> created = post("/iam/users",
                Map.of("username", username, "password", password,
                        "firstName", "T", "lastName", "keeper",
                        "email", username + "@test.local", "roles", Set.of("STORE_PERSON")),
                Map.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).as("provision STORE_PERSON user").isTrue();
        String keeperUid = (String) created.getBody().get("uid");

        ResponseEntity<Map> assigned = post(
                "/masterdata/stores/uid/" + MAIN_STORE_UID + "/staff",
                Map.of("userUid", keeperUid), Map.class);
        assertThat(assigned.getStatusCode().value()).isEqualTo(201);

        // Log in AS the keeper and query their stores.
        String keeperToken = login(username, password);
        ResponseEntity<List<Map<String, Object>>> mine = getAsList(keeperToken, "/masterdata/stores/mine");
        assertThat(mine.getStatusCode().is2xxSuccessful()).isTrue();

        List<Map<String, Object>> stores = mine.getBody();
        assertThat(stores).hasSize(1);
        assertThat(stores.get(0).get("uid")).isEqualTo(MAIN_STORE_UID);
        // Same summary shape as the store list endpoint.
        assertThat(stores.get(0)).containsKeys("uid", "code", "name", "active");
        // The unaffiliated store is not returned.
        assertThat(stores).noneMatch(s -> CONS_STORE_UID.equals(s.get("uid")));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unaffiliatedUserGetsEmptyList() {
        // A fresh STORE_PERSON with no affiliations sees no stores (the ROOT/admin
        // case behaves the same — no special-casing).
        String password = "Passw0rd!123";
        String username = "loner" + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Map> created = post("/iam/users",
                Map.of("username", username, "password", password,
                        "firstName", "T", "lastName", "loner",
                        "email", username + "@test.local", "roles", Set.of("STORE_PERSON")),
                Map.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).isTrue();

        String token = login(username, password);
        ResponseEntity<List<Map<String, Object>>> mine = getAsList(token, "/masterdata/stores/mine");
        assertThat(mine.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(mine.getBody()).isEmpty();
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        ResponseEntity<String> response = rest.getForEntity("/masterdata/stores/mine", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    // ----- helpers -----------------------------------------------------------

    private String login(String username, String password) {
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(username, password), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("Login failed for " + username);
        }
        return login.tokens().accessToken();
    }

    private ResponseEntity<List<Map<String, Object>>> getAsList(String token, String path) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(h),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
