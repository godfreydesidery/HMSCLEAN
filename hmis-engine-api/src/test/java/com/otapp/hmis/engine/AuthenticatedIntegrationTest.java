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
        return tokenForRoles("approver", java.util.Set.of("ROOT"));
    }

    private String sharedClinicianUsername;
    private String sharedClinicianUid;

    /**
     * Provisions a CLINICIAN (once per test instance) and affiliates it with the
     * given clinic, returning its username. Booking a consultation now requires
     * the clinician to hold the CLINICIAN role and be assigned to the clinic, so
     * tests use this instead of the bootstrap ROOT user. Idempotent and
     * multi-clinic — call it per clinic the same clinician should serve.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected String clinicianAffiliatedWith(String clinicUid) {
        if (sharedClinicianUsername == null) {
            String username = "clin" + Long.toString(System.nanoTime(), 36);
            ResponseEntity<java.util.Map> created = post(
                    "/iam/users",
                    java.util.Map.of(
                            "username", username,
                            "password", "Clinician!123",
                            "firstName", "Test",
                            "lastName", "Clinician",
                            "email", username + "@test.local",
                            "roles", java.util.Set.of("CLINICIAN")),
                    java.util.Map.class);
            if (!created.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Failed to provision clinician: " + created.getStatusCode());
            }
            sharedClinicianUsername = username;
            sharedClinicianUid = (String) created.getBody().get("uid");
        }
        ResponseEntity<java.util.Map> assigned = post(
                "/masterdata/clinics/uid/" + clinicUid + "/clinicians",
                java.util.Map.of("userUid", sharedClinicianUid),
                java.util.Map.class);
        if (!assigned.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to affiliate clinician with clinic " + clinicUid
                    + ": " + assigned.getStatusCode());
        }
        return sharedClinicianUsername;
    }

    /**
     * Drives a freshly-booked consultation to IN_PROGRESS so clinical entries
     * (notes, orders, prescriptions) can be authored — legacy
     * {@code open_consultation} confines authoring to IN_PROGRESS. Settles the
     * consultation-fee invoice first (the CASH fee gate) when it still carries a
     * balance, paying in the invoice's own currency, then opens the consultation.
     * A zero-fee (covered / follow-up) consultation settles at booking, so the
     * payment step is skipped.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void openConsultation(String consultationUid) {
        ResponseEntity<java.util.Map> invoiceResp = get(
                "/billing/consultations/uid/" + consultationUid + "/invoice", java.util.Map.class);
        if (invoiceResp.getStatusCode().is2xxSuccessful() && invoiceResp.getBody() != null) {
            java.util.Map<String, Object> invoice = invoiceResp.getBody();
            Object balanceRaw = invoice.get("balance");
            java.math.BigDecimal balance = balanceRaw == null
                    ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(balanceRaw.toString());
            if (balance.signum() > 0) {
                ResponseEntity<java.util.Map> paid = post(
                        "/billing/invoices/uid/" + invoice.get("uid") + "/payments",
                        java.util.Map.of("method", "CASH", "amount", balance,
                                "currency", String.valueOf(invoice.get("currency"))),
                        java.util.Map.class);
                if (!paid.getStatusCode().is2xxSuccessful()) {
                    throw new IllegalStateException("Failed to settle consultation fee for "
                            + consultationUid + ": " + paid.getStatusCode());
                }
            }
        }
        ResponseEntity<java.util.Map> started = post(
                "/encounters/consultations/uid/" + consultationUid + "/start", null, java.util.Map.class);
        if (!started.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to open consultation "
                    + consultationUid + ": " + started.getStatusCode());
        }
    }

    /** Provision a fresh, uniquely-named user with the given roles and return its access token. */
    protected String tokenForRoles(String prefix, java.util.Set<String> roles) {
        String username = prefix + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Object> created = post(
                "/iam/users",
                java.util.Map.of(
                        "username", username,
                        "password", "Secondary!123",
                        "firstName", "Test",
                        "lastName", prefix,
                        "email", username + "@test.local",
                        "roles", roles),
                Object.class);
        if (!created.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to provision user: " + created.getStatusCode());
        }
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(username, "Secondary!123"), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("Login failed for " + username);
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

    /** GET as a specific bearer token (not the default ROOT). */
    protected <T> ResponseEntity<T> getAs(String token, String path, Class<T> type) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(h), type);
    }

    /**
     * Access token for the shared CLINICIAN provisioned by
     * {@link #clinicianAffiliatedWith(String)}. Needed when a test must query a
     * clinician-scoped endpoint (e.g. the reception queue) AS the clinician who
     * owns the consultation, rather than as ROOT.
     */
    protected String sharedClinicianToken() {
        if (sharedClinicianUsername == null) {
            throw new IllegalStateException("Call clinicianAffiliatedWith(...) before sharedClinicianToken()");
        }
        LoginResponse login = rest.postForObject(
                "/auth/login", new LoginRequest(sharedClinicianUsername, "Clinician!123"), LoginResponse.class);
        if (login == null || login.tokens() == null) {
            throw new IllegalStateException("Login failed for shared clinician " + sharedClinicianUsername);
        }
        return login.tokens().accessToken();
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
     * Settles any outstanding admission (ward-bed) invoice so the discharge
     * bill-clearance gate clears. Admitting to a priced ward now issues + arms a
     * ward-bed bill at admit time (process-audit cluster #1 / M23), so a test that
     * admits and then discharges must clear that bill first. No-op when there is no
     * admission invoice or nothing is outstanding.
     */
    @SuppressWarnings("rawtypes")
    protected void settleAdmissionBill(String admissionUid) {
        ResponseEntity<java.util.Map> r =
                get("/billing/admissions/uid/" + admissionUid + "/invoice", java.util.Map.class);
        java.util.Map body = r.getBody();
        if (!r.getStatusCode().is2xxSuccessful() || body == null || body.get("balance") == null) {
            return;
        }
        java.math.BigDecimal balance = new java.math.BigDecimal(body.get("balance").toString());
        if (balance.signum() <= 0) {
            return;
        }
        post("/billing/invoices/uid/" + body.get("uid") + "/payments",
                java.util.Map.of("method", "CASH", "amount", balance, "currency", body.get("currency")),
                java.util.Map.class);
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
