package com.trading.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtValidator Unit Tests")
class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private String secret;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        // Same base64-encoded secret used in application.yml
        secret = Base64.getEncoder().encodeToString(
                "this-is-a-dev-only-secret-key-please-change-in-production".getBytes());
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        jwtValidator = new JwtValidator(secret);
    }

    private String buildToken(long expiryMs, String userId, String role) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", "test@trading.dev")
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("isValid: returns true for a valid unexpired token")
    void isValid_ReturnsTrueForValidToken() {
        String token = buildToken(900_000, UUID.randomUUID().toString(), "ROLE_USER");
        assertThat(jwtValidator.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid: returns false for an expired token")
    void isValid_ReturnsFalseForExpiredToken() {
        String token = buildToken(-1000, UUID.randomUUID().toString(), "ROLE_USER"); // already expired
        assertThat(jwtValidator.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("isValid: returns false for a malformed token")
    void isValid_ReturnsFalseForMalformedToken() {
        assertThat(jwtValidator.isValid("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("isValid: returns false for empty string")
    void isValid_ReturnsFalseForEmptyString() {
        assertThat(jwtValidator.isValid("")).isFalse();
    }

    @Test
    @DisplayName("extractUserId: returns correct subject from valid token")
    void extractUserId_ReturnsSubject() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken(900_000, userId, "ROLE_USER");
        assertThat(jwtValidator.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("extractRole: returns correct role claim")
    void extractRole_ReturnsRole() {
        String token = buildToken(900_000, UUID.randomUUID().toString(), "ROLE_ADMIN");
        assertThat(jwtValidator.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("extractEmail: returns correct email claim")
    void extractEmail_ReturnsEmail() {
        String token = buildToken(900_000, UUID.randomUUID().toString(), "ROLE_USER");
        assertThat(jwtValidator.extractEmail(token)).isEqualTo("test@trading.dev");
    }
}
