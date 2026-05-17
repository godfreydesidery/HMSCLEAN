package com.otapp.hmis.engine.iam.application;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.iam.application.dto.LoginRequest;
import com.otapp.hmis.engine.iam.application.dto.LoginResponse;
import com.otapp.hmis.engine.iam.application.dto.TokenPair;
import com.otapp.hmis.engine.iam.domain.User;
import com.otapp.hmis.engine.iam.domain.UserRepository;
import com.otapp.hmis.engine.iam.infrastructure.jwt.JwtTokenService;
import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<String> roles = IamMapper.roleNames(user);
        List<String> privileges = IamMapper.privilegeNames(user);

        String access = tokenService.issueAccessToken(user.getUsername(), roles, privileges);
        String refresh = tokenService.issueRefreshToken(user.getUsername());

        return new LoginResponse(TokenPair.bearer(access, refresh), IamMapper.toSummary(user), roles, privileges);
    }

    @Transactional(readOnly = true)
    public TokenPair refresh(String refreshToken) {
        Claims claims = tokenService.parse(refreshToken);
        if (tokenService.typeOf(claims) != JwtTokenService.TokenType.REFRESH) {
            throw new BadCredentialsException("Not a refresh token");
        }

        User user = userRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User disabled");
        }

        List<String> roles = IamMapper.roleNames(user);
        List<String> privileges = IamMapper.privilegeNames(user);

        String access = tokenService.issueAccessToken(user.getUsername(), roles, privileges);
        String newRefresh = tokenService.issueRefreshToken(user.getUsername());
        return TokenPair.bearer(access, newRefresh);
    }
}
