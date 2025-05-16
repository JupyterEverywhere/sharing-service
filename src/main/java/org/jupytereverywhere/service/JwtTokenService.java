package org.jupytereverywhere.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class JwtTokenService {

    public static final String SESSION_ID = "session_id";
    public static final String NOTEBOOK_ID = "notebook_id";
    public static final String NOTEBOOK_PASSWORD = "notebook_password";
    private final SecretKey secretKey;
    private final int expirationMinutes;

    public JwtTokenService(@Value("${security.jwt.token.secret-key}") String secretKey,
        @Value("${security.jwt.token.expiration-minutes}") int expirationMinutes,
        PasswordEncoder passwordEncoder) {
        this.secretKey = createSecretKey(secretKey);
        this.expirationMinutes = expirationMinutes;
    }

    private SecretKey createSecretKey(String secretKeyString) {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(String sessionId) {
        return generateToken(sessionId, null);
    }

    public String generateToken(String sessionId, String notebookId) {
        JwtBuilder jwtBuilder = Jwts.builder()
            .claim(SESSION_ID, sessionId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L))
            .signWith(secretKey, SignatureAlgorithm.HS256);

        if (notebookId != null) {
            jwtBuilder.claim(NOTEBOOK_ID, notebookId);
        }

        return jwtBuilder.compact();
    }

    public UUID extractSessionIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("The token is null or empty");
        }

        try {
            Claims claims = extractAllClaims(token);
            String sessionId = claims.get(SESSION_ID, String.class);

            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException(
                    "Invalid JWT token: the session_id claim is missing or empty");
            }

            return UUID.fromString(sessionId);
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired, extracting session ID from claims: {}", e.getClaims());
            String sessionId = e.getClaims().get(SESSION_ID, String.class);
            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException(
                    "Invalid JWT token: the session_id claim is missing or empty");
            }
            return UUID.fromString(sessionId);
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public String extractNotebookIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Claims claims = extractAllClaims(token);
            return claims.get(NOTEBOOK_ID, String.class);
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired, extracting notebook ID from claims: {}", e.getClaims());
            return e.getClaims().get(NOTEBOOK_ID, String.class);
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public String sanitizeToken(String token) {
        return token.replaceFirst("(?i)^bearer\\s*", "").trim();
    }

    Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired, returning claims: {}", e.getClaims());
            return e.getClaims();
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}
