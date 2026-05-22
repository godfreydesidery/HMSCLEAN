package com.otapp.hmis.engine.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Phase 42 — HR asset register lifecycle:
 *
 *   Create asset → look up by tag → retire (RETIRED) → reinstate →
 *   retire (DISPOSED) → assert DISPOSED is terminal (further retire
 *   transitions rejected).
 */
class AssetRegisterIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void createRetireReinstateDisposeLifecycle() {
        String tag = "ULTRA-" + System.nanoTime();

        // 1. Create. (Map.of caps at 10 entries — use ofEntries for the full record.)
        Map asset = expectOk(post(
                "/hr/assets",
                Map.ofEntries(
                        Map.entry("tag",               tag),
                        Map.entry("name",              "Ultrasound machine"),
                        Map.entry("category",          "Diagnostic equipment"),
                        Map.entry("location",          "Radiology suite 2"),
                        Map.entry("serialNo",          "US-998877"),
                        Map.entry("manufacturer",      "MediCorp"),
                        Map.entry("model",             "X-200"),
                        Map.entry("acquisitionDate",   LocalDate.now().minusYears(2).toString()),
                        Map.entry("acquisitionCost",   "12500000.00"),
                        Map.entry("currency",          "TZS"),
                        Map.entry("custodianUsername", "root")),
                Map.class));
        String assetUid = (String) asset.get("uid");
        assertThat(asset.get("status")).isEqualTo("ACTIVE");

        // 2. By-tag lookup.
        Map byTag = expectOk(get("/hr/assets/by-tag/" + tag, Map.class));
        assertThat(byTag.get("uid")).isEqualTo(assetUid);

        // 3. Retire (RETIRED, recoverable).
        Map retired = expectOk(post(
                "/hr/assets/uid/" + assetUid + "/retire",
                Map.of(
                        "target", "RETIRED",
                        "date",   LocalDate.now().toString(),
                        "reason", "Pending repair"),
                Map.class));
        assertThat(retired.get("status")).isEqualTo("RETIRED");
        assertThat(retired.get("retiredReason")).isEqualTo("Pending repair");

        // 4. Reinstate back to ACTIVE.
        Map active = expectOk(post(
                "/hr/assets/uid/" + assetUid + "/reinstate",
                null,
                Map.class));
        assertThat(active.get("status")).isEqualTo("ACTIVE");
        assertThat(active.get("retiredAt")).isNull();

        // 5. Dispose.
        Map disposed = expectOk(post(
                "/hr/assets/uid/" + assetUid + "/retire",
                Map.of(
                        "target", "DISPOSED",
                        "date",   LocalDate.now().toString(),
                        "reason", "Sold to clinic XYZ"),
                Map.class));
        assertThat(disposed.get("status")).isEqualTo("DISPOSED");

        // 6. DISPOSED is terminal — further retire transitions are refused.
        ResponseEntity<Map> denied = post(
                "/hr/assets/uid/" + assetUid + "/retire",
                Map.of(
                        "target", "RETIRED",
                        "date",   LocalDate.now().toString()),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("DISPOSED is terminal — retire should be refused")
                .isTrue();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void duplicateTagRejected() {
        String tag = "DUP-" + System.nanoTime();
        expectOk(post("/hr/assets",
                Map.of("tag", tag, "name", "First"),
                Map.class));
        ResponseEntity<Map> dup = post("/hr/assets",
                Map.of("tag", tag, "name", "Second"),
                Map.class);
        assertThat(dup.getStatusCode().is4xxClientError()).isTrue();
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
