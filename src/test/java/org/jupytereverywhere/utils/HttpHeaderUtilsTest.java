package org.jupytereverywhere.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.springframework.http.HttpHeaders;

class HttpHeaderUtilsTest {

  @Test
  void testCreateAuthorizationHeader_WithValidToken() {
    String token = "validJwtTokenString";
    HttpHeaders headers = HttpHeaderUtils.createAuthorizationHeader(token);

    assertNotNull(headers, "Headers should not be null");
    assertTrue(headers.containsKey("Authorization"), "Headers should contain Authorization key");
    assertEquals("Bearer " + token, headers.getFirst("Authorization"), "Authorization header value should be correct");
  }

  @Test
  void testCreateAuthorizationHeader_WithEmptyToken() {
    String token = "";
    HttpHeaders headers = HttpHeaderUtils.createAuthorizationHeader(token);

    assertNotNull(headers, "Headers should not be null");
    assertTrue(headers.containsKey("Authorization"), "Headers should contain Authorization key");
    assertEquals("Bearer ", headers.getFirst("Authorization"), "Authorization header value should be 'Bearer '");
  }

  @Test
  void testCreateAuthorizationHeader_WithNullToken() {
    String token = null;
    HttpHeaders headers = HttpHeaderUtils.createAuthorizationHeader(token);

    assertNotNull(headers, "Headers should not be null");
    assertTrue(headers.containsKey("Authorization"), "Headers should contain Authorization key");
    assertEquals("Bearer null", headers.getFirst("Authorization"), "Authorization header value should be 'Bearer null'");
  }

  @Test
  void testGetDomainFromRequest_WithXForwardedForHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 192.168.1.2");
    when(request.getRemoteAddr()).thenReturn("8.8.8.8");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertNotNull(domain, "Domain should not be null");
    assertEquals("192.168.1.1", domain, "Domain should match the first IP in X-Forwarded-For header");
  }

  @Test
  void testGetDomainFromRequest_WithXRealIpHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.3");
    when(request.getRemoteAddr()).thenReturn("8.8.8.8");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertNotNull(domain, "Domain should not be null");
    assertEquals("192.168.1.3", domain, "Domain should match the X-Real-IP header");
  }

  @Test
  void testGetDomainFromRequest_WithRemoteAddrFallback() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("8.8.8.8");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertNotNull(domain, "Domain should not be null");
    assertTrue(domain.equals("8.8.8.8") || domain.equals("dns.google"),
        "Domain should match the remote address or resolve to the DNS hostname");
  }

  @Test
  void testResolveHostName_WithValidIp() {
    String hostName = HttpHeaderUtils.resolveHostName("8.8.8.8");

    assertNotNull(hostName, "Host name should not be null");
    assertTrue(hostName.equals("8.8.8.8") || !hostName.isEmpty(),
        "Host name should either match the IP or resolve to a valid hostname");
  }

  @Test
  void testResolveHostName_WithUnknownHostException() {
    String hostName = HttpHeaderUtils.resolveHostName("invalid-ip");

    assertEquals("Unknown", hostName, "Host name should be 'Unknown' for invalid IP addresses");
  }

  @Test
  void testGetDomainFromRequest_WithInvalidHeaders() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
    when(request.getHeader("X-Real-IP")).thenReturn("unknown");
    when(request.getRemoteAddr()).thenReturn("192.168.0.1");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertEquals("192.168.0.1", domain, "Domain should fall back to remote address if headers are invalid");
  }

  @Test
  void testGetDomainFromRequest_WithNoHeaders() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertEquals("localhost", domain, "Domain should resolve to 'localhost' for 127.0.0.1");
  }

  @Test
  void testGetHeaderValue_WithValidHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Test-Header")).thenReturn("HeaderValue");

    String headerValue = HttpHeaderUtils.getHeaderValue(request, "X-Test-Header");

    assertEquals("HeaderValue", headerValue, "Header value should match the expected value");
  }

  @Test
  void testGetHeaderValue_WithNullHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Test-Header")).thenReturn(null);

    String headerValue = HttpHeaderUtils.getHeaderValue(request, "X-Test-Header");

    assertNull(headerValue, "Header value should be null for a missing header");
  }

  @Test
  void testGetHeaderValue_WithEmptyHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Test-Header")).thenReturn("");

    String headerValue = HttpHeaderUtils.getHeaderValue(request, "X-Test-Header");

    assertNull(headerValue, "Header value should be null for an empty header");
  }

  @Test
  void testGetHeaderValue_WithUnknownHeader() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Test-Header")).thenReturn("unknown");

    String headerValue = HttpHeaderUtils.getHeaderValue(request, "X-Test-Header");

    assertNull(headerValue, "Header value should be null for an 'unknown' header value");
  }
}
