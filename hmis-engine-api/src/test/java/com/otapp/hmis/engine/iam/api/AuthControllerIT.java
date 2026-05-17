package com.otapp.hmis.engine.iam.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AbstractIntegrationTest;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void rootCanLoginAndReceivesPrivileges() {
        ResponseEntity<LoginResponse> response = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("root", "TestRoot!123"),
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.tokens().accessToken()).isNotBlank();
        assertThat(body.tokens().refreshToken()).isNotBlank();
        assertThat(body.user().username()).isEqualTo("root");
        assertThat(body.roles()).contains("ROOT");
        assertThat(body.privileges()).contains("USER_READ", "USER_CREATE", "ROLE_READ");
    }

    @Test
    void invalidPasswordRejectedAsUnauthorized() {
        ResponseEntity<String> response = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("root", "WrongPassword!"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
