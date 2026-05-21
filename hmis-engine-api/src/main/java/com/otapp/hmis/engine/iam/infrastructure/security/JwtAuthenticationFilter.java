package com.otapp.hmis.engine.iam.infrastructure.security;

import com.otapp.hmis.engine.iam.domain.RevokedTokenRepository;
import com.otapp.hmis.engine.iam.infrastructure.jwt.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenService tokenService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final DomainUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length()).trim();
        try {
            Claims claims = tokenService.parse(token);
            if (tokenService.typeOf(claims) != JwtTokenService.TokenType.ACCESS) {
                chain.doFilter(request, response);
                return;
            }
            String jti = tokenService.jtiOf(claims);
            if (jti != null && revokedTokenRepository.existsByJti(jti)) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            // Re-validate the subject against the database on every request rather than
            // trusting the token's embedded claims. This makes disabling, deleting,
            // locking, or changing the roles of a user take effect immediately instead
            // of lingering until the (up to 1-hour) access token expires. A deleted
            // subject raises UsernameNotFoundException, caught below.
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails.getUsername(), null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            // Invalid/expired token, or the subject no longer exists — leave the
            // context empty so the request is treated as unauthenticated.
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
