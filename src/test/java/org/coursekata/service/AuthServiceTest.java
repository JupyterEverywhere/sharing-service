package org.coursekata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.coursekata.exception.TokenRefreshException;
import org.coursekata.model.TokenStore;
import org.coursekata.model.auth.AuthenticationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

  @Mock
  private JwtTokenService jwtTokenService;

  @Mock
  private TokenStore tokenStore;

  @InjectMocks
  private AuthService authService;

  @Test
  void testGenerateInitialTokenResponse_Success() {
    String expectedToken = "some-jwt-token";
    when(jwtTokenService.generateToken(anyString())).thenReturn(expectedToken);

    AuthenticationResponse response = authService.generateInitialTokenResponse();

    assertNotNull(response);
    assertEquals(expectedToken, response.getToken());
    verify(jwtTokenService).generateToken(anyString());
    verify(tokenStore).storeToken(any(UUID.class), eq(expectedToken));
  }

  @Test
  void testRefreshTokenResponse_Success() {
    String oldToken = "old-jwt-token";
    UUID sessionId = UUID.randomUUID();
    String expectedToken = "new-jwt-token";

    when(tokenStore.getToken(sessionId)).thenReturn(oldToken);
    when(jwtTokenService.extractSessionIdFromToken(oldToken)).thenReturn(sessionId);
    when(jwtTokenService.validateToken(oldToken)).thenReturn(true);
    when(jwtTokenService.generateToken(sessionId.toString())).thenReturn(expectedToken);

    AuthenticationResponse response = authService.refreshTokenResponse(oldToken);

    assertNotNull(response);
    assertEquals(expectedToken, response.getToken());
    verify(jwtTokenService).extractSessionIdFromToken(oldToken);
    verify(jwtTokenService).generateToken(sessionId.toString());
    verify(tokenStore).removeToken(sessionId);
    verify(tokenStore).storeToken(sessionId, expectedToken);
  }

  @Test
  void testRefreshTokenResponse_Failure_InvalidSessionId() {
    String invalidToken = "invalid-jwt-token";

    when(jwtTokenService.extractSessionIdFromToken(invalidToken)).thenReturn(null);

    assertThrows(TokenRefreshException.class, () -> authService.refreshTokenResponse(invalidToken));
    verify(jwtTokenService).extractSessionIdFromToken(invalidToken);
    verify(jwtTokenService, never()).generateToken(anyString());
    verify(tokenStore, never()).removeToken(any(UUID.class));
    verify(tokenStore, never()).storeToken(any(UUID.class), anyString());
  }

  @Test
  void testRefreshTokenResponse_Failure_TokenValidation() {
    String invalidToken = "invalid-jwt-token";
    UUID sessionId = UUID.randomUUID();

    when(jwtTokenService.extractSessionIdFromToken(invalidToken)).thenReturn(sessionId);
    when(jwtTokenService.validateToken(invalidToken)).thenReturn(false);

    assertThrows(TokenRefreshException.class, () -> authService.refreshTokenResponse(invalidToken));
    verify(jwtTokenService).extractSessionIdFromToken(invalidToken);
    verify(jwtTokenService, never()).generateToken(anyString());
    verify(tokenStore, never()).removeToken(any(UUID.class));
    verify(tokenStore, never()).storeToken(any(UUID.class), anyString());
  }
}
