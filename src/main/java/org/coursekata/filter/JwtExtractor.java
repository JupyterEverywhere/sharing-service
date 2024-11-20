package org.coursekata.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class JwtExtractor {
  private static final String MESSAGE_KEY = "Message";

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
}
