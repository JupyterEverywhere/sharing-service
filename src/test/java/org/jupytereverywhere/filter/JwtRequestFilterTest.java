package org.jupytereverywhere.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.filter.JwtExtractor;
import org.jupytereverywhere.filter.JwtRequestFilter;
import org.jupytereverywhere.filter.JwtValidator;
import org.jupytereverywhere.service.JwtTokenService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
    assertTrue(response.getErrorMessage().contains("JWT Token has expired"));

    verify(filterChain, never()).doFilter(request, response);
  }
}
