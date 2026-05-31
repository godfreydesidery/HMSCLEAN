package com.otapp.hmis.engine.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The JWT filter re-validates the subject against the database on every request
 * (JwtAuthenticationFilter), so changes to an account take effect immediately
 * rather than lingering until the access token expires:
 *  - disabling the user blocks the still-valid token at once (401);
 *  - removing a privilege via a role change is enforced at once (403).
 */
class ImmediateRevocationIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void disablingAUserBlocksItsStillValidTokenImmediately() {
        Provisioned u = provision(Set.of("ADMIN")); // ADMIN carries USER_READ

        // The fresh token can read users.
        assertThat(getAs(u.token, "/iam/users", Map.class).getStatusCode().is2xxSuccessful())
                .as("ADMIN token may list users before being disabled")
                .isTrue();

        // Root disables the account.
        ResponseEntity<Map> disabled = put(
                "/iam/users/uid/" + u.uid + "/enabled", Map.of("enabled", false), Map.class);
        assertThat(disabled.getStatusCode().is2xxSuccessful()).isTrue();

        // The very same (unexpired) token is now rejected — no waiting for TTL.
        assertThat(getAs(u.token, "/iam/users", Map.class).getStatusCode())
                .as("disabled user's token must be rejected immediately")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void removingAPrivilegeViaRoleChangeIsEnforcedImmediately() {
        Provisioned u = provision(Set.of("ADMIN")); // has USER_READ

        assertThat(getAs(u.token, "/iam/users", Map.class).getStatusCode().is2xxSuccessful())
                .as("ADMIN token may list users before its roles change")
                .isTrue();

        // Swap to a role that lacks USER_READ.
        ResponseEntity<Map> roles = put(
                "/iam/users/uid/" + u.uid + "/roles", Set.of("RECEPTION"), Map.class);
        assertThat(roles.getStatusCode().is2xxSuccessful()).isTrue();

        // Token still authenticates (user exists + enabled) but authorities are
        // re-derived from the database, so USER_READ is gone -> 403.
        assertThat(getAs(u.token, "/iam/users", Map.class).getStatusCode())
                .as("removed privilege must be enforced immediately")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ----- helpers -----------------------------------------------------------

    private record Provisioned(String uid, String token) {}

    @SuppressWarnings("rawtypes")
    private Provisioned provision(Set<String> roles) {
        String username = "revoke" + Long.toString(System.nanoTime(), 36);
        String password = "Secret!1234";
        ResponseEntity<Map> created = post("/iam/users", Map.of(
                "username", username, "password", password,
                "firstName", "Rev", "lastName", "Oke",
                "email", username + "@test.local", "roles", roles), Map.class);
        if (!created.getStatusCode().is2xxSuccessful() || created.getBody() == null) {
            throw new IllegalStateException("provision failed: " + created.getStatusCode());
        }
        String uid = (String) created.getBody().get("uid");
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(username, password), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("login failed for " + username);
        }
        return new Provisioned(uid, login.tokens().accessToken());
    }
}
