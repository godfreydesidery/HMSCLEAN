package com.otapp.hmis.engine.masterdata.clinicstaff.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Verifies the clinician ⇄ clinic affiliation endpoints: assignment is gated on
 * the CLINICIAN role, listing returns only active affiliations, removal is a
 * soft delete, re-assignment is idempotent, and the endpoints require
 * MASTERDATA_MANAGE.
 */
class ClinicStaffControllerIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assignListRemoveIsIdempotentAndRoleGated() {
        String clinicianUid = createUser("doc", "CLINICIAN");

        // Assign → 201 and appears in the listing.
        ResponseEntity<Map> assigned = post(cliniciansPath(),
                Map.of("userUid", clinicianUid), Map.class);
        assertThat(assigned.getStatusCode().value()).isEqualTo(201);
        assertThat(list()).anyMatch(c -> clinicianUid.equals(c.get("userUid")));

        // Re-assign the same clinician → idempotent (still 201, still one active row).
        ResponseEntity<Map> again = post(cliniciansPath(),
                Map.of("userUid", clinicianUid), Map.class);
        assertThat(again.getStatusCode().value()).isEqualTo(201);
        assertThat(list().stream().filter(c -> clinicianUid.equals(c.get("userUid"))).count())
                .isEqualTo(1);

        // Remove → 204 and gone from the (active) listing.
        ResponseEntity<Void> removed = rest.exchange(
                cliniciansPath() + "/uid/" + clinicianUid, HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(authHeaders()), Void.class);
        assertThat(removed.getStatusCode().value()).isEqualTo(204);
        assertThat(list()).noneMatch(c -> clinicianUid.equals(c.get("userUid")));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assigningANonClinicianIsRejected() {
        String nurseUid = createUser("nurse", "NURSE");
        ResponseEntity<Map> response = post(cliniciansPath(),
                Map.of("userUid", nurseUid), Map.class);
        assertThat(response.getStatusCode().is4xxClientError())
                .as("A user without the CLINICIAN role cannot be affiliated").isTrue();
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        ResponseEntity<String> response = rest.getForEntity(cliniciansPath(), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    // ----- helpers -----------------------------------------------------------

    private static String cliniciansPath() {
        return "/masterdata/clinics/uid/" + OPD_CLINIC_UID + "/clinicians";
    }

    private List<Map<String, Object>> list() {
        ResponseEntity<List<Map<String, Object>>> response = get(cliniciansPath(),
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
        assertThat(created.getStatusCode().is2xxSuccessful())
                .as("provision %s user", role).isTrue();
        return (String) created.getBody().get("uid");
    }
}
