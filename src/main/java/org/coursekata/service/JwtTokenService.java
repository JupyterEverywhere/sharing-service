package org.coursekata.service;

import io.jsonwebtoken.JwtBuilder;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(String sessionId) {
        return generateToken(sessionId, null);
    }

    public String generateToken(String sessionId, String notebookId) {
        JwtBuilder jwtBuilder = Jwts.builder()
            .claim("session_id", sessionId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L))
            .signWith(secretKey, SignatureAlgorithm.HS256);

        if (notebookId != null) {
            jwtBuilder.claim("notebook_id", notebookId);
        }

        return jwtBuilder.compact();
    }

    public UUID extractSessionIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("El token es nulo o está vacío");
        }

        try {
            Claims claims = extractAllClaims(token);
            String sessionId = claims.get("session_id", String.class);

            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException(
                    "Token JWT inválido: el claim session_id falta o está vacío");
            }

            return UUID.fromString(sessionId);
        } catch (ExpiredJwtException e) {
            log.warn("Token ha expirado, extrayendo session ID de los claims: {}", e.getClaims());
            String sessionId = e.getClaims().get("session_id", String.class);
            if (sessionId == null || sessionId.isEmpty()) {
                throw new IllegalArgumentException(
                    "Token JWT inválido: el claim session_id falta o está vacío");
            }
            return UUID.fromString(sessionId);
        } catch (JwtException e) {
            log.error("Token JWT inválido: {}", e.getMessage());
            throw new IllegalArgumentException("Token JWT inválido", e);
        }
    }

    public String extractNotebookIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Claims claims = extractAllClaims(token);
            return claims.get("notebook_id", String.class);
        } catch (ExpiredJwtException e) {
            log.warn("Token ha expirado, extrayendo notebook ID de los claims: {}", e.getClaims());
            return e.getClaims().get("notebook_id", String.class);
        } catch (JwtException e) {
            log.error("Token JWT inválido: {}", e.getMessage());
            throw new IllegalArgumentException("Token JWT inválido", e);
        }
    }

    public String extractNotebookPasswordFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Claims claims = extractAllClaims(token);
            return claims.get("notebook_password", String.class);
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired, extracting notebook password from claims: {}", e.getClaims());
            return e.getClaims().get("notebook_password", String.class);
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
            log.error("La validación del token falló: {}", e.getMessage());
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
            log.error("Fallo al verificar la expiración del token: {}", e.getMessage());
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
            log.warn("Token expirado, retornando claims: {}", e.getClaims());
            return e.getClaims();
        } catch (JwtException e) {
            log.error("Token JWT inválido: {}", e.getMessage());
            throw new IllegalArgumentException("Token JWT inválido", e);
        }
    }
}
