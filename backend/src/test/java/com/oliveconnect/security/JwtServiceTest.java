package com.oliveconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "super-secure-secret-key-at-least-256-bits-long-for-testing-sfax-2026",
                3600000,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Génération et validation d'un jeton JWT valide")
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken("user-123", "test@sfax.tn", List.of("AGRICULTEUR"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@sfax.tn");
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
        assertThat(jwtService.extractRoles(token)).containsExactly("AGRICULTEUR");
    }

    @Test
    @DisplayName("Validation d'un jeton invalide ou corrompu")
    void testValidateInvalidToken() {
        assertThat(jwtService.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("Jeton expiré : rejeté par validateToken")
    void testExpiredToken() {
        JwtService expiredJwtService = new JwtService(
                "super-secure-secret-key-at-least-256-bits-long-for-testing-sfax-2026",
                -1000, // already expired
                new ObjectMapper()
        );

        String expiredToken = expiredJwtService.generateToken("user-123", "test@sfax.tn", List.of("AGRICULTEUR"));
        assertThat(expiredJwtService.validateToken(expiredToken)).isFalse();
    }
}
