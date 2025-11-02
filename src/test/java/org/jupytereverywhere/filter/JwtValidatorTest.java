package org.jupytereverywhere.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.service.JwtTokenService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtValidatorTest {

  @Mock private JwtTokenService jwtTokenService;

  private JwtValidator jwtValidator;

  @BeforeEach
  void setUp() {
    jwtValidator = new JwtValidator(jwtTokenService);
  }

  @Test
  void testIsValid_ValidToken() {
    String validToken = "validTokenString";
    when(jwtTokenService.validateToken(validToken)).thenReturn(true);

    assertTrue(jwtValidator.isValid(validToken));
  }

  @Test
  void testIsValid_InvalidToken() {
    String invalidToken = "invalidTokenString";
    when(jwtTokenService.validateToken(invalidToken)).thenReturn(false);

    assertFalse(jwtValidator.isValid(invalidToken));
  }

  @Test
  void testIsValid_ExceptionDuringValidation() {
    String token = "someTokenString";
    when(jwtTokenService.validateToken(token)).thenThrow(new JwtException("Invalid JWT token"));

    assertThrows(JwtException.class, () -> jwtValidator.isValid(token));
  }
}
