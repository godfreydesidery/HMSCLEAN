package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import com.otapp.hmis.engine.iam.application.dto.TokenPair;
import com.otapp.hmis.engine.iam.domain.LoginAttempt;
import com.otapp.hmis.engine.iam.domain.LoginAttemptRepository;
import com.otapp.hmis.engine.iam.domain.RevokedToken;
import com.otapp.hmis.engine.iam.domain.RevokedTokenRepository;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.iam.infrastructure.jwt.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LoginAttemptRepository attemptRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtTokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String username = request.username();
        User user = userRepository.findByUsername(username).orElse(null);

        // Pre-auth gates: reject locked/disabled accounts without exposing
        // whether the credentials would have been valid.
        if (user != null && user.isLocked()) {
            attemptRepository.save(new LoginAttempt(username, LoginAttempt.Outcome.USER_LOCKED, ipAddress, userAgent));
            throw new LockedException("Account is temporarily locked. Try again later.");
        }
        if (user != null && !user.isEnabled()) {
            attemptRepository.save(new LoginAttempt(username, LoginAttempt.Outcome.USER_DISABLED, ipAddress, userAgent));
            throw new DisabledException("Account is disabled");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException ex) {
            LoginAttempt.Outcome outcome = user == null
                    ? LoginAttempt.Outcome.UNKNOWN_USER
                    : LoginAttempt.Outcome.BAD_CREDENTIALS;
            attemptRepository.save(new LoginAttempt(username, outcome, ipAddress, userAgent));
            if (user != null) {
                registerFailedAttempt(user);
            }
            throw new BadCredentialsException("Invalid username or password");
        }

        // Successful auth — user must exist at this point.
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        attemptRepository.save(new LoginAttempt(username, LoginAttempt.Outcome.SUCCESS, ipAddress, userAgent));

        List<String> roles = IamMapper.roleNames(user);
        List<String> privileges = IamMapper.privilegeNames(user);
        String access = tokenService.issueAccessToken(user.getUsername(), roles, privileges);
        String refresh = tokenService.issueRefreshToken(user.getUsername());
        return new LoginResponse(
                TokenPair.bearer(access, refresh),
                IamMapper.toSummary(user),
                roles,
                privileges,
                user.isPasswordMustChange());
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        Claims claims = tokenService.parse(refreshToken);
        if (tokenService.typeOf(claims) != JwtTokenService.TokenType.REFRESH) {
            throw new BadCredentialsException("Not a refresh token");
        }

        String jti = tokenService.jtiOf(claims);
        if (jti != null && revokedTokenRepository.existsByJti(jti)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        User user = userRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User disabled");
        }
        if (user.isLocked()) {
            throw new LockedException("Account is temporarily locked");
        }
        // Tokens issued before the user's last password change are invalid —
        // changing the password forces all existing sessions to re-authenticate.
        Instant iat = tokenService.issuedAtOf(claims);
        if (user.getPasswordChangedAt() != null && iat != null
                && iat.isBefore(user.getPasswordChangedAt())) {
            throw new BadCredentialsException("Refresh token predates a password change");
        }

        // Rotate: revoke the old refresh token so a stolen copy can't be reused.
        if (jti != null) {
            Instant exp = tokenService.expirationOf(claims);
            revokedTokenRepository.save(new RevokedToken(
                    jti, user.getUsername(), exp == null ? Instant.now().plusSeconds(86_400) : exp));
        }

        List<String> roles = IamMapper.roleNames(user);
        List<String> privileges = IamMapper.privilegeNames(user);
        String access = tokenService.issueAccessToken(user.getUsername(), roles, privileges);
        String newRefresh = tokenService.issueRefreshToken(user.getUsername());
        return TokenPair.bearer(access, newRefresh);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        // Best-effort: parse failures are swallowed so logout always succeeds.
        try {
            Claims claims = tokenService.parse(refreshToken);
            String jti = tokenService.jtiOf(claims);
            if (jti == null || revokedTokenRepository.existsByJti(jti)) return;
            Instant exp = tokenService.expirationOf(claims);
            revokedTokenRepository.save(new RevokedToken(
                    jti, claims.getSubject(), exp == null ? Instant.now().plusSeconds(86_400) : exp));
        } catch (RuntimeException ignored) {
            // Token already invalid — nothing to revoke.
        }
    }

    /**
     * Self-service password change. Requires the user's current password
     * even when they were forced into the change by an admin reset — so
     * a stolen access token cannot finalise a take-over. Bumps
     * {@code passwordChangedAt} which invalidates every existing refresh
     * token on next /auth/refresh.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), currentPassword));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        validateNewPassword(newPassword, currentPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordMustChange(false);
    }

    private void registerFailedAttempt(User user) {
        int next = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(next);
        if (next >= SecurityPolicy.MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(SecurityPolicy.LOCKOUT_DURATION));
        }
    }

    private static void validateNewPassword(String newPassword, String currentPassword) {
        if (newPassword == null || newPassword.length() < SecurityPolicy.MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("Password must be at least "
                    + SecurityPolicy.MIN_PASSWORD_LENGTH + " characters long");
        }
        if (newPassword.equals(currentPassword)) {
            throw new BusinessRuleException("New password must be different from the current password");
        }
    }
}
