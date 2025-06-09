package org.jupytereverywhere.filter;

import org.springframework.stereotype.Component;

import org.jupytereverywhere.service.JwtTokenService;

@Component
public class JwtValidator {
  private final JwtTokenService jwtTokenService;

  
  public JwtValidator(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  public boolean isValid(String jwt) {
    return jwtTokenService.validateToken(jwt);
  }
}

