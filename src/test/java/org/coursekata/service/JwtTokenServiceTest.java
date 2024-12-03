package org.coursekata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class JwtTokenServiceTest {

  private JwtTokenService jwtTokenService;
  private SecretKey secretKey;
  private String validToken;
  private UUID sessionId;

  @BeforeEach
  void setUp() {
    String secretKeyString = "testSecretKeyForJwtTokenService1234567890";
    secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    jwtTokenService = new JwtTokenService(secretKeyString, 60, passwordEncoder);
    sessionId = UUID.randomUUID();

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
  void testGenerateToken_WithNotebookId() {
    String notebookId = "notebook-12345";
    String token = jwtTokenService.generateToken(sessionId.toString(), notebookId);

    assertNotNull(token);

    UUID extractedSessionId = jwtTokenService.extractSessionIdFromToken(token);
    String extractedNotebookId = jwtTokenService.extractNotebookIdFromToken(token);

    assertEquals(sessionId, extractedSessionId);
    assertEquals(notebookId, extractedNotebookId);
  }

  @Test
  void testExtractNotebookIdFromToken() {
    String notebookId = "notebook-12345";
    String token = jwtTokenService.generateToken(sessionId.toString(), notebookId);

    String extractedNotebookId = jwtTokenService.extractNotebookIdFromToken(token);
    assertEquals(notebookId, extractedNotebookId);
  }

  @Test
  void testExtractNotebookIdFromToken_TokenWithoutNotebookId() {
    String token = jwtTokenService.generateToken(sessionId.toString());

    String extractedNotebookId = jwtTokenService.extractNotebookIdFromToken(token);
    assertNull(extractedNotebookId);
  }

  @Test
  void testExtractSessionIdFromToken_ExpiredToken() {
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
  void testValidateToken_ValidAndExpiredTokens() {
    boolean isValid = jwtTokenService.validateToken(validToken);
    assertTrue(isValid);

    Map<String, Object> claims = new HashMap<>();
    claims.put("session_id", sessionId.toString());

    String expiredToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
        .setExpiration(new Date(System.currentTimeMillis() - 1000 * 30))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    boolean isExpired = jwtTokenService.validateToken(expiredToken);
    assertFalse(isExpired);
  }

  @Test
  void testSanitizeToken_WithVariousPrefixes() {
    String token = "Bearer " + validToken;
    String sanitizedToken = jwtTokenService.sanitizeToken(token);
    assertEquals(validToken, sanitizedToken);

    String tokenNoSpace = "bearer" + validToken;
    sanitizedToken = jwtTokenService.sanitizeToken(tokenNoSpace);
    assertEquals(validToken, sanitizedToken);

    String tokenNoPrefix = validToken;
    sanitizedToken = jwtTokenService.sanitizeToken(tokenNoPrefix);
    assertEquals(validToken, sanitizedToken);
  }

  @Test
  void testExtractAllClaims() {
    String notebookId = "notebook-12345";
    String token = jwtTokenService.generateToken(sessionId.toString(), notebookId);

    Claims claims = jwtTokenService.extractAllClaims(token);
    assertNotNull(claims);
    assertEquals(sessionId.toString(), claims.get("session_id"));
    assertEquals(notebookId, claims.get("notebook_id"));
  }

  @Test
  void testIsTokenExpired_ValidAndExpiredTokens() {
    boolean isExpired = jwtTokenService.isTokenExpired(validToken);
    assertFalse(isExpired);

    Map<String, Object> claims = new HashMap<>();
    claims.put("session_id", sessionId.toString());

    String expiredToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
        .setExpiration(new Date(System.currentTimeMillis() - 1000 * 30))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();

    isExpired = jwtTokenService.isTokenExpired(expiredToken);
    assertTrue(isExpired);
  }

  @Test
  void testGenerateToken() {
    String token = jwtTokenService.generateToken(sessionId.toString());
    assertNotNull(token);

    UUID extractedSessionId = jwtTokenService.extractSessionIdFromToken(token);
    assertEquals(sessionId, extractedSessionId);
  }

  @Test
  void testExtractExpiration() {
    Date expirationDate = jwtTokenService.extractExpiration(validToken);
    assertNotNull(expirationDate);
    assertTrue(expirationDate.after(new Date()));
  }
}
