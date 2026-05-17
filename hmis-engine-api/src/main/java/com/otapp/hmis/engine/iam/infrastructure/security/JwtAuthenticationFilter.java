package com.otapp.hmis.engine.iam.infrastructure.security;

import com.otapp.hmis.engine.iam.domain.RevokedTokenRepository;
import com.otapp.hmis.engine.iam.infrastructure.jwt.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

            String username = claims.getSubject();
            List<GrantedAuthority> authorities = Stream.concat(
                            tokenService.rolesOf(claims).stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)),
                            tokenService.privilegesOf(claims).stream().map(SimpleGrantedAuthority::new))
                    .distinct()
                    .map(GrantedAuthority.class::cast)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
