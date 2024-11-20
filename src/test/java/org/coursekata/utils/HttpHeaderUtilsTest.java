package org.coursekata.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
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
  void testGetDomainFromRequest_WithValidIp() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("8.8.8.8");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertNotNull(domain, "Domain should not be null");
  }

  @Test
  void testGetDomainFromRequest_WithLocalhostIp() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertNotNull(domain, "Domain should not be null");
    assertEquals("localhost", domain, "Domain should be 'localhost' for 127.0.0.1");
  }

  @Test
  void testGetDomainFromRequest_WithUnknownIp() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("256.256.256.256");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertEquals("Unknown", domain, "Domain should be 'Unknown' for an invalid IP address");
  }

  @Test
  void testGetDomainFromRequest_WithInvalidHostName() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("invalid-ip-address");

    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    assertEquals("Unknown", domain, "Domain should be 'Unknown' for an invalid host name");
  }
}
