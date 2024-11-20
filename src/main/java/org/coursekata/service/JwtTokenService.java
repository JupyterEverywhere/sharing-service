package org.coursekata.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Log4j2
@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final int expirationMinutes;

    public JwtTokenService(@Value("${security.jwt.token.secret-key}") String secretKey,
        @Value("${security.jwt.token.expiration-minutes}") int expirationMinutes) {
        this.secretKey = createSecretKey(secretKey);
        this.expirationMinutes = expirationMinutes;
    }

    private SecretKey createSecretKey(String secretKeyString) {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }

    public String generateToken(String sessionId) {
        return Jwts.builder()
            .claim("session_id", sessionId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public UUID extractSessionIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token is null or empty");
        }

        try {
            Claims claims = extractAllClaims(token);
            String sessionId = claims.get("session_id", String.class);

            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException("Invalid JWT token: session_id claim is missing or empty");
            }

            return UUID.fromString(sessionId);
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired, extracting session ID from claims: {}", e.getClaims());
            String sessionId = e.getClaims().get("session_id", String.class);
            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException("Invalid JWT token: session_id claim is missing or empty");
            }
            return UUID.fromString(sessionId);
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
            log.warn("Token expired, returning claims: {}", e.getClaims());
            return e.getClaims();
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}
