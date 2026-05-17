package com.otapp.hmis.engine.iam.api;

import com.otapp.hmis.engine.iam.application.AuthService;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import com.otapp.hmis.engine.iam.application.dto.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Exchange username and password for access + refresh tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, clientIp(httpRequest), userAgent(httpRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Issue a new access+refresh token pair from a valid refresh token")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Principal principal) {
        authService.changePassword(principal.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the supplied refresh token so it cannot be exchanged again")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(min = 1, max = 128) String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {}

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return req.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        if (ua == null) return null;
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
