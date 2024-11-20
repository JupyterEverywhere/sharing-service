package org.coursekata.filter;

import org.coursekata.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtValidator {
  private final JwtTokenService jwtTokenService;

  @Autowired
  public JwtValidator(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  public boolean isValid(String jwt) {
    return jwtTokenService.validateToken(jwt);
  }
}

