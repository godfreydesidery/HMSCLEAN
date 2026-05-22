package com.otapp.hmis.engine.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

/**
 * The clinician specialty / registration profile: upserting it is reflected on
 * the user's profile endpoint and enriches the staff-by-role projection so the
 * UI can render "Dr. X — Cardiology".
 */
class ProviderProfileIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void upsertSurfacesOnProfileAndStaffByRole() {
        String username = "doc" + Long.toString(System.nanoTime(), 36);
        ResponseEntity<Map> created = post("/iam/users",
                Map.of("username", username, "password", "Passw0rd!123",
                        "firstName", "Cardio", "lastName", "Doc",
                        "email", username + "@test.local", "roles", Set.of("CLINICIAN")),
                Map.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).isTrue();
        String userUid = (String) created.getBody().get("uid");

        // Upsert the profile.
        ResponseEntity<Map> profile = put("/iam/users/uid/" + userUid + "/provider-profile",
                Map.of("specialty", "Cardiology", "registrationNo", "REG-9", "licenseNo", "LIC-9"),
                Map.class);
        assertThat(profile.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(profile.getBody().get("specialty")).isEqualTo("Cardiology");

        // It is reflected on the staff-by-role projection consumed by pickers.
        ResponseEntity<List<Map<String, Object>>> staff = get("/iam/staff/by-role/CLINICIAN",
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertThat(staff.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(staff.getBody())
                .anyMatch(s -> username.equals(s.get("username"))
                        && "Cardiology".equals(s.get("specialty")));
    }
}
