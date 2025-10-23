package org.jupytereverywhere.filter;

import org.apache.logging.log4j.message.StringMapMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class JwtExtractor {
  private static final String MESSAGE_KEY = "Message";

  @Value("${security.extra-auth-header.name:}")
  String extraAuthHeaderName;

  @Value("${security.extra-auth-header.secret:}")
  String extraAuthHeaderSecret;

  public String getExtraAuthHeaderName() {
    return extraAuthHeaderName;
  }

  public String getExtraAuthHeaderSecret() {
    return extraAuthHeaderSecret;
  }

  public String extractJwtFromRequest(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.toLowerCase().startsWith("bearer ")) {
      String jwt = header.substring(7).trim();
      if (jwt.isEmpty()) {
        return null;
      }
      log.debug(new StringMapMessage().with(MESSAGE_KEY, "JWT Token detected"));
      return jwt;
    }
    return null;
  }

  public boolean validateExtraAuthHeader(HttpServletRequest request) {
    // Only enforce extra auth if both name and secret are set and not empty
    if (extraAuthHeaderName == null
        || extraAuthHeaderName.trim().isEmpty()
        || extraAuthHeaderSecret == null
        || extraAuthHeaderSecret.trim().isEmpty()) {
      return true; // Not configured, always pass
    }

    String headerValue = request.getHeader(extraAuthHeaderName);
    if (headerValue == null) {
      return false; // Configured, but header missing
    }
    if (headerValue.isEmpty()) {
      log.debug(new StringMapMessage().with(MESSAGE_KEY, "Extra auth header present but empty"));
      return false;
    }
    boolean isValid = extraAuthHeaderSecret.equals(headerValue);
    if (isValid) {
      log.debug(
          new StringMapMessage().with(MESSAGE_KEY, "Extra auth header validation successful"));
    } else {
      log.debug(new StringMapMessage().with(MESSAGE_KEY, "Extra auth header validation failed"));
    }
    return isValid;
  }
}
