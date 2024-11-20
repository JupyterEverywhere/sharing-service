package org.coursekata.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import org.springframework.http.HttpHeaders;

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
    String clientIp = request.getRemoteAddr();
    logInfo("Received request from client IP", CLIENT_IP_KEY, clientIp);
    return resolveHostName(clientIp);
  }

  private static String resolveHostName(String clientIp) {
    try {
      InetAddress inetAddress = InetAddress.getByName(clientIp);
      String hostName = inetAddress.getHostName();
      logInfo("Resolved host name", CLIENT_IP_KEY, hostName);
      return hostName.equals(clientIp) ? clientIp : hostName;
    } catch (UnknownHostException e) {
      logError(clientIp, e);
      return "Unknown";
    }
  }

  private static void logInfo(String message, String key, String value) {
    log.info(new StringMapMessage().with(MESSAGE_KEY, message).with(key, value));
  }

  private static void logError(String value, Exception e) {
    log.error(new StringMapMessage().with(MESSAGE_KEY, "Failed to resolve host name")
        .with(CLIENT_IP_KEY, value), e);
  }
}
