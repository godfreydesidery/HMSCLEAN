package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Phase 39 end-to-end test for lab / radiology attachments:
 *
 *   Register an OUTSIDER patient → raise an outsider lab order → upload a
 *   small "result" file via multipart → list → download → assert the
 *   bytes round-trip → delete → assert the list is empty.
 *
 * Uses the seeded CBC lab test (V4).
 */
class OrderAttachmentIT extends AuthenticatedIntegrationTest {

    private static final String CBC_LAB_TEST_UID = "01J5KQRPCD0000000000000LB1";

    @Test
    @SuppressWarnings("rawtypes")
    void uploadListDownloadDeleteRoundTrip() {
        // 1. Set up a patient + outsider lab order.
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   "Att",
                        "lastName",    "Achment",
                        "dateOfBirth", LocalDate.now().minusYears(25).toString(),
                        "gender",      "MALE",
                        "type",        "OUTSIDER",
                        "paymentType", "CASH"),
                Map.class)).get("uid");
        String orderUid = (String) expectOk(post(
                "/encounters/patients/uid/" + patientUid + "/outsider-orders",
                Map.of(
                        "kind",       "LAB_TEST",
                        "serviceUid", CBC_LAB_TEST_UID,
                        "urgency",    "NORMAL"),
                Map.class)).get("uid");

        // 2. Upload a small file via multipart/form-data.
        byte[] payload = "WBC 5.6\nHGB 14.1\nPLT 220\n".getBytes(StandardCharsets.UTF_8);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return "cbc-result.txt";
            }
        };
        form.add("file", fileResource);
        form.add("description", "Hand-typed result summary");

        HttpHeaders multipart = authHeaders();
        multipart.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> uploadResp = rest.exchange(
                "/encounters/orders/uid/" + orderUid + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(form, multipart),
                Map.class);
        Map uploaded = expectOk(uploadResp);
        String attachmentUid = (String) uploaded.get("uid");
        assertThat(uploaded.get("orderUid")).isEqualTo(orderUid);
        assertThat(uploaded.get("filename")).isEqualTo("cbc-result.txt");
        assertThat(uploaded.get("orderKind")).isEqualTo("LAB_TEST");
        assertThat(((Number) uploaded.get("sizeBytes")).longValue()).isEqualTo(payload.length);

        // 3. List shows exactly one row.
        ResponseEntity<List<Map<String, Object>>> listResp = get(
                "/encounters/orders/uid/" + orderUid + "/attachments",
                new ParameterizedTypeReference<>() {});
        assertThat(listResp.getStatusCode().is2xxSuccessful()).isTrue();
        List<Map<String, Object>> list = listResp.getBody();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("uid")).isEqualTo(attachmentUid);

        // 4. Download returns the bytes verbatim.
        ResponseEntity<byte[]> dlResp = rest.exchange(
                "/encounters/attachments/uid/" + attachmentUid + "/download",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                byte[].class);
        assertThat(dlResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(dlResp.getBody()).isEqualTo(payload);
        assertThat(dlResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .as("Content-Disposition must surface the original filename")
                .contains("cbc-result.txt");

        // 5. Delete and confirm the list is empty.
        ResponseEntity<Void> delResp = rest.exchange(
                "/encounters/attachments/uid/" + attachmentUid,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);
        assertThat(delResp.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<List<Map<String, Object>>> afterDelete = get(
                "/encounters/orders/uid/" + orderUid + "/attachments",
                new ParameterizedTypeReference<>() {});
        assertThat(afterDelete.getBody()).isEmpty();
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
