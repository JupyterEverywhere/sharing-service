package org.jupytereverywhere.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.message.StringMapMessage;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HttpHeaderUtils {

  private static final String MESSAGE_KEY = "Message";
  private static final String CLIENT_IP_KEY = "ClientIP";

  private HttpHeaderUtils() {}

  public static HttpHeaders createAuthorizationHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    return headers;
  }

  public static String getDomainFromRequest(HttpServletRequest request) {
    String clientIp = extractClientIp(request);
    logInfo("Received request from client IP", CLIENT_IP_KEY, clientIp);
    return resolveHostName(clientIp);
  }

  private static String extractClientIp(HttpServletRequest request) {
    String clientIp = getHeaderValue(request, "X-Forwarded-For");
    if (clientIp != null) {
      return clientIp.split(",")[0].trim();
    }
    clientIp = getHeaderValue(request, "X-Real-IP");
    return (clientIp != null) ? clientIp : request.getRemoteAddr();
  }

  static String getHeaderValue(HttpServletRequest request, String headerName) {
    String headerValue = request.getHeader(headerName);
    if (headerValue == null || headerValue.isEmpty() || "unknown".equalsIgnoreCase(headerValue)) {
      return null;
    }
    return headerValue;
  }

  static String resolveHostName(String clientIp) {
    try {
      InetAddress inetAddress = InetAddress.getByName(clientIp);
      String hostName = inetAddress.getHostName();
      logInfo("Resolved host name", CLIENT_IP_KEY, hostName);

      // If the hostname is the same as the input and it's not a valid IP address,
      // it means the resolution failed and we should return "Unknown"
      if (hostName.equals(clientIp) && !isValidIpAddress(clientIp)) {
        return "Unknown";
      }

      return hostName.equals(clientIp) ? clientIp : hostName;
    } catch (UnknownHostException e) {
      logError(clientIp, e);
      return "Unknown";
    }
  }

  private static boolean isValidIpAddress(String ip) {
    try {
      InetAddress inetAddress = InetAddress.getByName(ip);
      // Check if it's a valid IP by verifying the hostname is an IP address format
      String hostAddress = inetAddress.getHostAddress();
      return hostAddress.equals(ip)
          || ip.matches(
              "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    } catch (UnknownHostException e) {
      return false;
    }
  }

  public static String getTokenFromRequest(HttpServletRequest request) {

    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }

    String tokenFromQuery = request.getParameter("token");
    if (tokenFromQuery != null && !tokenFromQuery.isEmpty()) {
      return tokenFromQuery;
    }

    throw new IllegalArgumentException("No token found in the request");
  }

  private static void logInfo(String message, String key, String value) {
    log.info(new StringMapMessage().with(MESSAGE_KEY, message).with(key, value));
  }

  private static void logError(String value, Exception e) {
    log.error(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Failed to resolve host name")
            .with(CLIENT_IP_KEY, value),
        e);
  }
}
