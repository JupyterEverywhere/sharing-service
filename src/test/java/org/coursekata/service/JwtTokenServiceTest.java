package org.coursekata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

  private JwtTokenService jwtTokenService;
  private SecretKey secretKey;
  private String validToken;
  private UUID sessionId;

  @BeforeEach
  void setUp() {
    String secretKeyString = "testSecretKeyForJwtTokenService1234567890";
    jwtTokenService = new JwtTokenService(secretKeyString, 60);
    sessionId = UUID.randomUUID();

    secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    Map<String, Object> claims = new HashMap<>();
    claims.put("session_id", sessionId.toString());

    validToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  @Test
  void testSanitizeToken() {
    String token = "Bearer " + validToken;
    String sanitizedToken = jwtTokenService.sanitizeToken(token);
    assertEquals(validToken, sanitizedToken);
  }

  @Test
  void testSanitizeToken_BearerNoSpace() {
    String token = "bearer" + validToken;
    String sanitizedToken = jwtTokenService.sanitizeToken(token);
    assertEquals(validToken, sanitizedToken);
  }

  @Test
  void testSanitizeToken_NoBearerPrefix() {
    String token = "NoPrefix" + validToken;
    String sanitizedToken = jwtTokenService.sanitizeToken(token);
    assertEquals(token.trim(), sanitizedToken);
  }

  @Test
  void testExtractSessionId() {
    UUID extractedSessionId = jwtTokenService.extractSessionIdFromToken(validToken);
    assertEquals(sessionId, extractedSessionId);
  }

  @Test
  void testExtractSessionId_ExpiredToken() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("session_id", sessionId.toString());

    String expiredToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
        .setExpiration(new Date(System.currentTimeMillis() - 1000 * 30))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    UUID extractedSessionId = jwtTokenService.extractSessionIdFromToken(expiredToken);
    assertEquals(sessionId, extractedSessionId);
  }

  @Test
  void testExtractSessionId_InvalidToken() {
    String invalidToken = "invalidTokenString";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      jwtTokenService.extractSessionIdFromToken(invalidToken);
    });

    assertTrue(exception.getMessage().contains("Token JWT inválido"));
  }

  @Test
  void testExtractSessionId_NoSessionIdClaim() {
    Map<String, Object> claims = new HashMap<>();

    String tokenWithoutSessionId = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      jwtTokenService.extractSessionIdFromToken(tokenWithoutSessionId);
    });

    assertTrue(exception.getMessage().contains("Token JWT inválido: el claim session_id falta o está vacío"));
  }

  @Test
  void testExtractSessionId_NullOrEmptyToken() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      jwtTokenService.extractSessionIdFromToken(null);
    });
    assertTrue(exception.getMessage().contains("El token es nulo o está vacío"));

    exception = assertThrows(IllegalArgumentException.class, () -> {
      jwtTokenService.extractSessionIdFromToken("");
    });
    assertTrue(exception.getMessage().contains("El token es nulo o está vacío"));
  }

  @Test
  void testIsTokenExpired_ValidToken() {
    boolean isExpired = jwtTokenService.isTokenExpired(validToken);
    assertFalse(isExpired);
  }

  @Test
  void testIsTokenExpired_ExpiredToken() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("session_id", sessionId.toString());

    String expiredToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
        .setExpiration(new Date(System.currentTimeMillis() - 1000 * 30))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    boolean isExpired = jwtTokenService.isTokenExpired(expiredToken);
    assertTrue(isExpired);
  }

  @Test
  void testIsTokenExpired_NullOrEmptyToken() {
    boolean isExpired = jwtTokenService.isTokenExpired(null);
    assertTrue(isExpired);

    isExpired = jwtTokenService.isTokenExpired("");
    assertTrue(isExpired);
  }

  @Test
  void testExtractExpiration() {
    Date expirationDate = jwtTokenService.extractExpiration(validToken);
    assertNotNull(expirationDate);
    assertTrue(expirationDate.after(new Date()));
  }

  @Test
  void testExtractExpiration_InvalidToken() {
    String invalidToken = "invalidTokenString";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      jwtTokenService.extractExpiration(invalidToken);
    });

    assertTrue(exception.getMessage().contains("Token JWT inválido"));
  }

  @Test
  void testGenerateToken() {
    String token = jwtTokenService.generateToken(sessionId.toString());
    assertNotNull(token);

    UUID extractedSessionId = jwtTokenService.extractSessionIdFromToken(token);
    assertEquals(sessionId, extractedSessionId);
  }

  @Test
  void testValidateToken_Valid() {
    boolean isValid = jwtTokenService.validateToken(validToken);
    assertTrue(isValid);
  }

  @Test
  void testValidateToken_ExpiredToken() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("session_id", sessionId.toString());

    String expiredToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
        .setExpiration(new Date(System.currentTimeMillis() - 1000 * 30))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    boolean isValid = jwtTokenService.validateToken(expiredToken);
    assertFalse(isValid);
  }

  @Test
  void testValidateToken_InvalidToken() {
    String invalidToken = "invalidTokenString";
    boolean isValid = jwtTokenService.validateToken(invalidToken);
    assertFalse(isValid);
  }

  @Test
  void testExtractSessionId_JwtException() {
    JwtException jwtException = new JwtException("Invalid JWT signature");

    JwtTokenService spyService = spy(jwtTokenService);
    doThrow(jwtException).when(spyService).extractAllClaims(anyString());

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      spyService.extractSessionIdFromToken("invalidToken");
    });

    assertTrue(exception.getMessage().contains("Token JWT inválido"));
  }

  @Test
  void testExtractAllClaims_ValidToken() {
    String validToken = Jwts.builder()
        .setSubject("testUser")
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    Claims claims = jwtTokenService.extractAllClaims(validToken);
    assertNotNull(claims);
    assertEquals("testUser", claims.getSubject());
  }

  @Test
  void testExtractAllClaims_InvalidToken() {
    String invalidToken = "invalidTokenString";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      jwtTokenService.extractAllClaims(invalidToken);
    });

    assertTrue(exception.getMessage().contains("Token JWT inválido"));
  }
}
