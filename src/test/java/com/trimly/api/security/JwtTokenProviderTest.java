package com.trimly.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT Token Provider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET = "9a3f5b7e2c8d1a4e6f0b3c5d7e9a1b3c5d7e9a1b3c5d7e9a1b3c5d7e9a1b3c5d";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid JWT token containing user claims")
    void shouldGenerateAndValidateToken() {
        Long userId = 42L;
        String email = "alex@trimly.io";

        String token = tokenProvider.generateToken(userId, email);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void shouldRejectMalformedToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

        assertThat(tokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Provider with negative expiration to simulate already-expired token
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000);
        String expiredToken = expiredProvider.generateToken(1L, "expired@trimly.io");

        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
    }
}
