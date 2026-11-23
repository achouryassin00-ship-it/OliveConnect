package com.oliveconnect.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final byte[] secretBytes;
    private final long expirationMs;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${jwt.secret:oliveconnect-secure-jwt-secret-key-sfax-2026-super-safe-minimum-256-bits}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            ObjectMapper objectMapper) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
        this.objectMapper = objectMapper;
    }

    public String generateToken(String userId, String email, List<String> roles) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (expirationMs / 1000);

            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", email);
            payload.put("userId", userId);
            payload.put("roles", roles);
            payload.put("iat", now);
            payload.put("exp", exp);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));

            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(dataToSign);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer le jeton JWT", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String dataToSign = parts[0] + "." + parts[1];
            String expectedSig = sign(dataToSign);
            if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            Map<String, Object> claims = parseClaims(parts[1]);
            Number exp = (Number) claims.get("exp");
            if (exp == null) return false;

            return Instant.now().getEpochSecond() < exp.longValue();
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        Map<String, Object> claims = getClaims(token);
        return claims != null ? (String) claims.get("sub") : null;
    }

    public String extractUserId(String token) {
        Map<String, Object> claims = getClaims(token);
        return claims != null ? (String) claims.get("userId") : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Map<String, Object> claims = getClaims(token);
        return claims != null ? (List<String>) claims.get("roles") : null;
    }

    private Map<String, Object> getClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            return parseClaims(parts[1]);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseClaims(String base64UrlPayload) throws Exception {
        byte[] bytes = Base64.getUrlDecoder().decode(base64UrlPayload);
        return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(rawHmac);
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
