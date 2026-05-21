package com.otapp.hmis.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Base for tests that need a real REST client authenticated as the
 * bootstrapped ROOT user. Handles the JWT dance up front so each test
 * can focus on the workflow it's exercising.
 */
public abstract class AuthenticatedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected ObjectMapper json;

    protected String accessToken;

    @BeforeEach
    void signInAsRoot() {
        LoginResponse login = rest.postForObject(
                "/auth/login",
                new LoginRequest("root", "TestRoot!123"),
                LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("ROOT login failed during test setup");
        }
        accessToken = login.tokens().accessToken();
    }

    protected HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /**
     * Provisions a fresh, uniquely-named user with the ROOT role (so it carries
     * every privilege) and returns an access token for it. Useful for workflows
     * that enforce segregation of duties — e.g. a discharge plan must be
     * approved by someone other than its author. The shared test container is
     * JVM-lifetime, so the username is made unique per call.
     */
    protected String secondUserToken() {
        String username = "approver" + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Object> created = post(
                "/iam/users",
                java.util.Map.of(
                        "username", username,
                        "password", "Approver!123",
                        "firstName", "Test",
                        "lastName", "Approver",
                        "email", username + "@test.local",
                        "roles", java.util.Set.of("ROOT")),
                Object.class);
        if (!created.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to provision second user: " + created.getStatusCode());
        }
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(username, "Approver!123"), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("Second-user login failed for " + username);
        }
        return login.tokens().accessToken();
    }

    /** POST as a specific bearer token (not the default ROOT). */
    protected <T> ResponseEntity<T> postAs(String token, String path, Object body, Class<T> type) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), type);
    }

    protected <T> ResponseEntity<T> post(String path, Object body, Class<T> type) {
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, authHeaders()), type);
    }

    protected <T> ResponseEntity<T> put(String path, Object body, Class<T> type) {
        return rest.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, authHeaders()), type);
    }

    protected <T> ResponseEntity<T> get(String path, Class<T> type) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(authHeaders()), type);
    }

    protected <T> ResponseEntity<T> get(String path, ParameterizedTypeReference<T> type) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(authHeaders()), type);
    }

    /**
     * Convenience to grab a single field from a JSON response without
     * binding a DTO class. Useful when the test only needs the uid of a
     * created resource.
     */
    @SuppressWarnings("unchecked")
    protected String stringField(Object body, String fieldName) {
        if (body == null) return null;
        if (body instanceof java.util.Map<?, ?> map) {
            Object v = map.get(fieldName);
            return v == null ? null : v.toString();
        }
        // Fall through: re-serialise then re-deserialise as Map. Slow but only
        // for tests that opt in via this helper.
        try {
            String raw = json.writeValueAsString(body);
            java.util.Map<String, Object> map =
                    json.readValue(raw, new TypeReference<java.util.Map<String, Object>>() {});
            Object v = map.get(fieldName);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
