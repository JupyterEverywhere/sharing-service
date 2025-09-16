package org.jupytereverywhere.filter;

import java.io.IOException;
import java.util.UUID;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.jupytereverywhere.service.JwtTokenService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

  @InjectMocks
  private JwtRequestFilter jwtRequestFilter;

  @Mock
  private JwtTokenService jwtTokenService;

  @Mock
  private JwtExtractor jwtExtractor;

  @Mock
  private JwtValidator jwtValidator;

  @Test
  void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
    String invalidToken = "invalidTokenString";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + invalidToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);


  // Legacy test: do not stub validateExtraAuthHeader, extra auth not configured
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(invalidToken);
    when(jwtValidator.isValid(invalidToken)).thenReturn(false);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator).isValid(invalidToken);
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain, never()).doFilter(request, response);
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  void testDoFilterInternal_ExpiredToken() throws ServletException, IOException {
    String expiredToken = "expiredTokenString";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + expiredToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);


  // Legacy test: do not stub validateExtraAuthHeader, extra auth not configured
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(expiredToken);
    when(jwtValidator.isValid(expiredToken)).thenReturn(false);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator).isValid(expiredToken);
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain, never()).doFilter(request, response);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  void testDoFilterInternal_ValidToken() throws ServletException, IOException {
    String validToken = "validTokenString";
    UUID sessionId = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + validToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);


  // Legacy test: do not stub validateExtraAuthHeader, extra auth not configured
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(validToken);
    when(jwtValidator.isValid(validToken)).thenReturn(true);
    when(jwtTokenService.extractSessionIdFromToken(validToken)).thenReturn(sessionId);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator).isValid(validToken);
    verify(jwtTokenService).extractSessionIdFromToken(validToken);
    verify(filterChain).doFilter(request, response);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testDoFilterInternal_NoToken() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);


  // Legacy test: do not stub validateExtraAuthHeader, extra auth not configured
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(null);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator, never()).isValid(anyString());
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilterInternal_NonBearerToken() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "NonBearerToken");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(null);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator, never()).isValid(anyString());
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilterInternal_ExceptionDuringProcessing() throws ServletException, IOException {
    String token = "someTokenString";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(token);
    when(jwtValidator.isValid(token)).thenThrow(new RuntimeException("Unexpected error"));

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator).isValid(token);
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain, never()).doFilter(request, response);
    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testDoFilterInternal_ExpiredJwtException() throws ServletException, IOException {
    String expiredToken = "expiredTokenString";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + expiredToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(expiredToken);
    when(jwtValidator.isValid(expiredToken)).thenThrow(new ExpiredJwtException(null, null, "Token has expired"));

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    String errorMessage = response.getErrorMessage();
    assertTrue(errorMessage != null && errorMessage.contains("JWT Token has expired"));

    verify(filterChain, never()).doFilter(request, response);
  }

  // Extra Auth Header Tests

  @Test
  void testDoFilterInternal_ExtraAuthValid_WithValidJWT_ShouldSucceed() throws ServletException, IOException {
  org.springframework.test.util.ReflectionTestUtils.setField(jwtExtractor, "extraAuthHeaderName", "X-Extra-Auth");
  org.springframework.test.util.ReflectionTestUtils.setField(jwtExtractor, "extraAuthHeaderSecret", "secret");
  String validToken = "validTokenString";
  UUID sessionId = UUID.randomUUID();
  MockHttpServletRequest request = new MockHttpServletRequest();
  request.addHeader("Authorization", "Bearer " + validToken);
  request.addHeader("X-Extra-Auth", "secret");
  when(jwtExtractor.validateExtraAuthHeader(request)).thenCallRealMethod();
  MockHttpServletResponse response = new MockHttpServletResponse();
  FilterChain filterChain = mock(FilterChain.class);

  when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(validToken);
  when(jwtValidator.isValid(validToken)).thenReturn(true);
  when(jwtTokenService.extractSessionIdFromToken(validToken)).thenReturn(sessionId);

  jwtRequestFilter.doFilterInternal(request, response, filterChain);

  assertTrue(jwtExtractor.validateExtraAuthHeader(request));
  verify(jwtValidator).isValid(validToken);
  verify(jwtTokenService).extractSessionIdFromToken(validToken);
  verify(filterChain).doFilter(request, response);
  assertEquals(200, response.getStatus());
  }

  @Test
  void testDoFilterInternal_ExtraAuthInvalid_WithValidJWT_ShouldFailOnExtraAuth() throws ServletException, IOException {
  jwtExtractor.extraAuthHeaderName = "X-Extra-Auth";
  jwtExtractor.extraAuthHeaderSecret = "secret";
  // Extra auth configured, should verify validateExtraAuthHeader
    String validToken = "validTokenString";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + validToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Mock extra auth validation to fail
    when(jwtExtractor.validateExtraAuthHeader(request)).thenReturn(false);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtExtractor).validateExtraAuthHeader(request);
    // JWT should never be checked when extra auth fails
    verify(jwtExtractor, never()).extractJwtFromRequest(request);
    verify(jwtValidator, never()).isValid(anyString());
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain, never()).doFilter(request, response);
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    String errorMessage = response.getErrorMessage();
    assertTrue(errorMessage != null && errorMessage.contains("Invalid authentication header"));
  }

  @Test
  void testDoFilterInternal_ExtraAuthValid_WithInvalidJWT_ShouldFailOnJWT() throws ServletException, IOException {
  jwtExtractor.extraAuthHeaderName = "X-Extra-Auth";
  jwtExtractor.extraAuthHeaderSecret = "secret";
  // Extra auth configured, should verify validateExtraAuthHeader
    String invalidToken = "invalidTokenString";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + invalidToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Extra auth passes, but JWT fails
    when(jwtExtractor.validateExtraAuthHeader(request)).thenReturn(true);
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(invalidToken);
    when(jwtValidator.isValid(invalidToken)).thenReturn(false);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtExtractor).validateExtraAuthHeader(request);
    verify(jwtExtractor).extractJwtFromRequest(request);
    verify(jwtValidator).isValid(invalidToken);
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain, never()).doFilter(request, response);
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  void testDoFilterInternal_ExtraAuthNotConfigured_WithValidJWT_ShouldWorkAsUsual() throws ServletException, IOException {
  // Extra auth not configured, should not verify validateExtraAuthHeader
    String validToken = "validTokenString";
    UUID sessionId = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + validToken);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Extra auth not configured (returns true to pass through)
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(validToken);
    when(jwtValidator.isValid(validToken)).thenReturn(true);
    when(jwtTokenService.extractSessionIdFromToken(validToken)).thenReturn(sessionId);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtValidator).isValid(validToken);
    verify(jwtTokenService).extractSessionIdFromToken(validToken);
    verify(filterChain).doFilter(request, response);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testDoFilterInternal_ExtraAuthValid_WithNoJWT_ShouldPassToPublicEndpoint() throws ServletException, IOException {
  jwtExtractor.extraAuthHeaderName = "X-Extra-Auth";
  jwtExtractor.extraAuthHeaderSecret = "secret";
  // Extra auth configured, should verify validateExtraAuthHeader
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Extra auth passes, but no JWT (for public endpoints)
    when(jwtExtractor.validateExtraAuthHeader(request)).thenReturn(true);
    when(jwtExtractor.extractJwtFromRequest(request)).thenReturn(null);

    jwtRequestFilter.doFilterInternal(request, response, filterChain);

    verify(jwtExtractor).validateExtraAuthHeader(request);
    verify(jwtExtractor).extractJwtFromRequest(request);
    verify(jwtValidator, never()).isValid(anyString());
    verify(jwtTokenService, never()).extractSessionIdFromToken(anyString());
    verify(filterChain).doFilter(request, response);
    assertEquals(200, response.getStatus());
  }
}
