package com.otapp.hmis.engine.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * External medical provider masterdata (DISCH-5). CRUD is gated on
 * MASTERDATA_MANAGE; the listing is also reachable by clinicians
 * (ENCOUNTER_ACCESS) so the referral closure form can offer a provider picker.
 */
class ExternalMedicalProviderIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void crudAndClinicianReadableSearch() {
        String code = "EMP" + Long.toString(System.nanoTime(), 36).toUpperCase();

        // Create (as ROOT / MASTERDATA_MANAGE).
        Map created = expectOk(post("/masterdata/external-providers",
                Map.of("code", code, "name", "Coast General Hospital",
                        "telephone", "+255711111111", "email", "info@coast.example"),
                Map.class));
        String uid = (String) created.get("uid");
        assertThat(created.get("code")).isEqualTo(code);
        assertThat(created.get("active")).isEqualTo(true);

        // Update mutates the name.
        Map updated = expectOk(put("/masterdata/external-providers/uid/" + uid,
                Map.of("name", "Coast General Referral Hospital"), Map.class));
        assertThat(updated.get("name")).isEqualTo("Coast General Referral Hospital");

        // Search finds it by query.
        Map page = expectOk(get("/masterdata/external-providers?query=" + code, Map.class));
        assertThat(((java.util.List<?>) page.get("content"))).isNotEmpty();

        // A clinician (ENCOUNTER_ACCESS, no MASTERDATA_MANAGE) can search...
        String clinicianToken = tokenForRoles("emprdr", Set.of("CLINICIAN"));
        ResponseEntity<Map> clinicianSearch = getAs(clinicianToken,
                "/masterdata/external-providers?query=" + code, Map.class);
        assertThat(clinicianSearch.getStatusCode().is2xxSuccessful())
                .as("Clinicians can list referral providers").isTrue();

        // ...but cannot create one.
        ResponseEntity<Map> clinicianCreate = postAs(clinicianToken,
                "/masterdata/external-providers",
                Map.of("code", code + "X", "name", "Nope"), Map.class);
        assertThat(clinicianCreate.getStatusCode().value())
                .as("Clinicians cannot create masterdata").isEqualTo(403);
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
