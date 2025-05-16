package org.jupytereverywhere.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.jupytereverywhere.service.JwtTokenService;

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

