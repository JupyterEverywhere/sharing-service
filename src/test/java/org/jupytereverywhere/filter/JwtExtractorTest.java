package org.jupytereverywhere.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.filter.JwtExtractor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class JwtExtractorTest {

  private final JwtExtractor jwtExtractor = new JwtExtractor();

  @Test
  void testExtractJwtFromRequest_WithValidBearerToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer validJwtTokenString");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNotNull(jwt);
    assertEquals("validJwtTokenString", jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithNoAuthorizationHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNull(jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithNonBearerToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "NonBearerToken");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNull(jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithBearerTokenAndExtraWhitespace() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer    validJwtTokenString   ");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNotNull(jwt);
    assertEquals("validJwtTokenString", jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithEmptyBearerToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer ");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNull(jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithMixedCaseBearer() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "bEaReR validJwtTokenString");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNotNull(jwt);
    assertEquals("validJwtTokenString", jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithNullAuthorizationHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn(null);

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNull(jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithMalformedAuthorizationHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn("BearerMalformed");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertNull(jwt);
  }

  @Test
  void testExtractJwtFromRequest_WithMultipleBearerTokens() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token1 Bearer token2");

    String jwt = jwtExtractor.extractJwtFromRequest(request);

    assertEquals("token1 Bearer token2", jwt);
  }
}

