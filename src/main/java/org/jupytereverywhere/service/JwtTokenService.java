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
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class JwtTokenService {

  public static final String SESSION_ID = "session_id";
  public static final String NOTEBOOK_ID = "notebook_id";
  public static final String NOTEBOOK_PASSWORD = "notebook_password";
  public static final String ROLE = "role";
  public static final String TOKEN_NAME = "token_name";
  public static final String ADMIN_ROLE = "ADMIN";
  private static final int MIN_SIGNING_KEY_BYTES = 32;
  private final SecretKey secretKey;
  private final JwtParser jwtParser;
  private final int expirationMinutes;

  public JwtTokenService(
      @Value("${security.jwt.token.secret-key}") String secretKey,
      @Value("${security.jwt.token.expiration-minutes}") int expirationMinutes,
      PasswordEncoder passwordEncoder) {
    if (expirationMinutes <= 0) {
      throw new IllegalArgumentException("JWT expiration must be greater than zero");
    }
    this.secretKey = createSecretKey(secretKey);
    this.jwtParser =
        Jwts.parserBuilder().setSigningKey(this.secretKey).setAllowedClockSkewSeconds(60).build();
    this.expirationMinutes = expirationMinutes;
  }

  private SecretKey createSecretKey(String secretKeyString) {
    if (secretKeyString == null || secretKeyString.isBlank()) {
      throw new IllegalArgumentException("JWT signing key must be configured");
    }
    byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_SIGNING_KEY_BYTES) {
      throw new IllegalArgumentException("JWT signing key must contain at least 32 UTF-8 bytes");
    }
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

  public String generateToken(String sessionId) {
    return generateToken(sessionId, null);
  }

  public String generateToken(String sessionId, String notebookId) {
    JwtBuilder jwtBuilder =
        Jwts.builder()
            .claim(SESSION_ID, sessionId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L))
            .signWith(secretKey, SignatureAlgorithm.HS256);

    if (notebookId != null) {
      jwtBuilder.claim(NOTEBOOK_ID, notebookId);
    }

    return jwtBuilder.compact();
  }

  public String generateAdminToken(String sessionId, String tokenName) {
    return Jwts.builder()
        .claim(SESSION_ID, sessionId)
        .claim(ROLE, ADMIN_ROLE)
        .claim(TOKEN_NAME, tokenName)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public String extractRoleFromToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return null;
    }
    try {
      Claims claims = extractAllClaims(token);
      return claims.get(ROLE, String.class);
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("JWT role extraction failed");
      return null;
    }
  }

  public String extractTokenNameFromToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return null;
    }
    try {
      Claims claims = extractAllClaims(token);
      return claims.get(TOKEN_NAME, String.class);
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("JWT token-name extraction failed");
      return null;
    }
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
      log.warn("Expired JWT session claim requested");
      String sessionId = e.getClaims().get(SESSION_ID, String.class);
      if (sessionId == null || sessionId.isEmpty()) {
        throw new IllegalArgumentException(
            "Invalid JWT token: the session_id claim is missing or empty");
      }
      return UUID.fromString(sessionId);
    } catch (JwtException e) {
      log.warn("JWT session extraction failed");
      throw new IllegalArgumentException("Invalid JWT token");
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
      log.warn("Expired JWT notebook claim requested");
      return e.getClaims().get(NOTEBOOK_ID, String.class);
    } catch (JwtException e) {
      log.warn("JWT notebook extraction failed");
      throw new IllegalArgumentException("Invalid JWT token");
    }
  }

  public boolean validateToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return !claims.getExpiration().before(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("JWT validation failed");
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
      log.warn("JWT expiration check failed");
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
      return jwtParser.parseClaimsJws(token).getBody();
    } catch (ExpiredJwtException e) {
      log.warn("Expired JWT claims requested");
      return e.getClaims();
    } catch (JwtException e) {
      log.warn("JWT parsing failed");
      throw new IllegalArgumentException("Invalid JWT token");
    }
  }
}
