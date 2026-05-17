package com.otapp.hmis.engine.masterdata.clinic.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AbstractIntegrationTest;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.ClinicDto;
import com.otapp.hmis.engine.masterdata.clinic.application.dto.CreateClinicRequest;
import com.otapp.hmis.engine.masterdata.clinic.domain.ClinicType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ClinicControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String accessToken;

    @BeforeEach
    void signIn() {
        LoginResponse login = rest.postForObject(
                "/api/auth/login",
                new LoginRequest("root", "TestRoot!123"),
                LoginResponse.class);
        accessToken = login.tokens().accessToken();
    }

    @Test
    void rootCanCreateUpdateAndDeactivateAClinic() {
        // Create
        CreateClinicRequest create = new CreateClinicRequest(
                "ENT", "Ear, Nose & Throat", ClinicType.SPECIALTY,
                "ENT specialist clinic", "Block C");
        ResponseEntity<ClinicDto> created = rest.exchange(
                "/api/masterdata/clinics",
                HttpMethod.POST,
                new HttpEntity<>(create, authHeaders()),
                ClinicDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ClinicDto saved = created.getBody();
        assertThat(saved).isNotNull();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.code()).isEqualTo("ENT");
        assertThat(saved.active()).isTrue();

        // Deactivate
        ResponseEntity<ClinicDto> deactivated = rest.exchange(
                "/api/masterdata/clinics/" + saved.id() + "/active",
                HttpMethod.PUT,
                new HttpEntity<>("{\"active\":false}", jsonHeaders()),
                ClinicDto.class);
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody().active()).isFalse();

        // Delete
        ResponseEntity<Void> deleted = rest.exchange(
                "/api/masterdata/clinics/" + saved.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/masterdata/clinics",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add("Content-Type", "application/json");
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = authHeaders();
        return h;
    }
}
