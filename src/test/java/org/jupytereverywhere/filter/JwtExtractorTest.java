package org.jupytereverywhere.filter;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  // Extra Auth Header Tests

  @Test
  void testValidateExtraAuthHeader_WhenNotConfigured_ShouldPassThrough() {
    JwtExtractor extractor = new JwtExtractor();
    // Default configuration: empty name and secret
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "");

    MockHttpServletRequest request = new MockHttpServletRequest();

    boolean result = extractor.validateExtraAuthHeader(request);

    assertTrue(result, "Should pass through when extra auth is not configured");
  }

  @Test
  void testValidateExtraAuthHeader_WhenNameEmpty_ShouldPassThrough() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "secret123");

    MockHttpServletRequest request = new MockHttpServletRequest();

    boolean result = extractor.validateExtraAuthHeader(request);

    assertTrue(result, "Should pass through when header name is empty");
  }

  @Test
  void testValidateExtraAuthHeader_WhenSecretEmpty_ShouldPassThrough() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "");

    MockHttpServletRequest request = new MockHttpServletRequest();

    boolean result = extractor.validateExtraAuthHeader(request);

    assertTrue(result, "Should pass through when secret is empty");
  }

  @Test
  void testValidateExtraAuthHeader_WithValidSecret_ShouldPass() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "secret123");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-origin-auth", "secret123");

    boolean result = extractor.validateExtraAuthHeader(request);

    assertTrue(result, "Should pass when extra auth header has correct secret");
  }

  @Test
  void testValidateExtraAuthHeader_WithInvalidSecret_ShouldFail() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "secret123");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-origin-auth", "wrongsecret");

    boolean result = extractor.validateExtraAuthHeader(request);

    assertFalse(result, "Should fail when extra auth header has wrong secret");
  }

  @Test
  void testValidateExtraAuthHeader_WhenHeaderMissing_ShouldFail() {
  JwtExtractor extractor = new JwtExtractor();
  ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
  ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "secret123");

  MockHttpServletRequest request = new MockHttpServletRequest();
  // No header added

  boolean result = extractor.validateExtraAuthHeader(request);

  assertFalse(result, "Should fail when extra auth header is missing and extra auth is configured");
  }

  @Test
  void testValidateExtraAuthHeader_WithEmptyHeaderValue_ShouldFail() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "secret123");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-origin-auth", "");

    boolean result = extractor.validateExtraAuthHeader(request);

    assertFalse(result, "Should fail when extra auth header value is empty");
  }

  @Test
  void testValidateExtraAuthHeader_WithDifferentHeaderName_ShouldWork() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-api-key");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "apikey123");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-api-key", "apikey123");

    boolean result = extractor.validateExtraAuthHeader(request);

    assertTrue(result, "Should work with different header names");
  }

  @Test
  void testValidateExtraAuthHeader_SecretIsCaseSensitive() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", "Secret123");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-origin-auth", "secret123");

    boolean result = extractor.validateExtraAuthHeader(request);

    assertFalse(result, "Secret validation should be case sensitive");
  }

  @Test
  void testValidateExtraAuthHeader_WithWhitespaceInSecret_ShouldNotTrim() {
    JwtExtractor extractor = new JwtExtractor();
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderName", "x-origin-auth");
    ReflectionTestUtils.setField(extractor, "extraAuthHeaderSecret", " secret123 ");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-origin-auth", " secret123 ");

    boolean result = extractor.validateExtraAuthHeader(request);

    assertTrue(result, "Should match exact secret including whitespace");
  }
}

