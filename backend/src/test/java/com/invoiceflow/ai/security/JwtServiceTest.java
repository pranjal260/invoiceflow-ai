package com.invoiceflow.ai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-must-be-at-least-32-characters";

    @Test
    void generatesTokenWithUserIdentityAndExpiration() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 3_600_000));
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId);

        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expiredTokenService = new JwtService(new JwtProperties(SECRET, -1));
        String token = expiredTokenService.generateToken(UUID.randomUUID());

        assertThatThrownBy(() -> expiredTokenService.extractUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
