package com.otapp.hmis.engine.iam.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    public enum TokenType { ACCESS, REFRESH }

    private static final String CLAIM_PRIVILEGES = "privileges";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties properties;

    public String issueAccessToken(String username, List<String> roles, List<String> privileges) {
        return buildToken(username, roles, privileges, TokenType.ACCESS, properties.accessTokenTtl().toMillis());
    }

    public String issueRefreshToken(String username) {
        return buildToken(username, List.of(), List.of(), TokenType.REFRESH, properties.refreshTokenTtl().toMillis());
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            throw new InvalidTokenException("Invalid or expired token", ex);
        }
    }

    public TokenType typeOf(Claims claims) {
        String raw = claims.get(CLAIM_TYPE, String.class);
        return raw == null ? TokenType.ACCESS : TokenType.valueOf(raw);
    }

    public String jtiOf(Claims claims) {
        return claims.getId();
    }

    public Instant issuedAtOf(Claims claims) {
        Date d = claims.getIssuedAt();
        return d == null ? null : d.toInstant();
    }

    public Instant expirationOf(Claims claims) {
        Date d = claims.getExpiration();
        return d == null ? null : d.toInstant();
    }

    @SuppressWarnings("unchecked")
    public List<String> rolesOf(Claims claims) {
        Object value = claims.get(CLAIM_ROLES);
        return value instanceof List<?> list ? (List<String>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> privilegesOf(Claims claims) {
        Object value = claims.get(CLAIM_PRIVILEGES);
        return value instanceof List<?> list ? (List<String>) list : List.of();
    }

    private String buildToken(String subject, List<String> roles, List<String> privileges, TokenType type, long ttlMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)))
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PRIVILEGES, privileges)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey key() {
        byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("hmis.security.jwt.secret must be at least 32 characters for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
