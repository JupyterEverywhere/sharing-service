package org.coursekata.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.coursekata.exception.TokenRefreshException;
import org.coursekata.model.auth.AuthenticationResponse;
import org.coursekata.model.auth.TokenRefreshRequest;
import org.coursekata.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private AuthController authController;

  @Mock
  private AuthService authService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    authController = new AuthController(authService);
  }

  @Test
  void testIssueToken_Success() {
    AuthenticationResponse expectedResponse = new AuthenticationResponse();
    expectedResponse.setToken("generated-token");

    when(authService.generateInitialTokenResponse()).thenReturn(expectedResponse);

    ResponseEntity<AuthenticationResponse> responseEntity = authController.issueToken();

    assertNotNull(responseEntity);
    assertEquals(200, responseEntity.getStatusCode().value());
    AuthenticationResponse responseBody = responseEntity.getBody();
    assertNotNull(responseBody);
    assertEquals("generated-token", responseBody.getToken());

    verify(authService, times(1)).generateInitialTokenResponse();
  }

  @Test
  void testIssueToken_Failure() {
    when(authService.generateInitialTokenResponse()).thenThrow(new RuntimeException("Token generation failed"));

    assertThrows(RuntimeException.class, () -> {
      authController.issueToken();
    });

    verify(authService, times(1)).generateInitialTokenResponse();
  }

  @Test
  void testRefreshToken_Success() {
    TokenRefreshRequest refreshRequest = new TokenRefreshRequest("valid-refresh-token");
    AuthenticationResponse expectedResponse = new AuthenticationResponse();
    expectedResponse.setToken("refreshed-token");

    when(authService.refreshTokenResponse(refreshRequest.getToken())).thenReturn(expectedResponse);

    ResponseEntity<AuthenticationResponse> responseEntity = authController.refreshToken(refreshRequest);

    assertNotNull(responseEntity);
    assertEquals(200, responseEntity.getStatusCode().value());
    AuthenticationResponse responseBody = responseEntity.getBody();
    assertNotNull(responseBody);
    assertEquals("refreshed-token", responseBody.getToken());

    verify(authService, times(1)).refreshTokenResponse(refreshRequest.getToken());
  }

  @Test
  void testRefreshToken_Failure() {
    TokenRefreshRequest refreshRequest = new TokenRefreshRequest("invalid-refresh-token");

    when(authService.refreshTokenResponse(refreshRequest.getToken())).thenThrow(new TokenRefreshException("Token refresh failed"));

    assertThrows(TokenRefreshException.class, () -> {
      authController.refreshToken(refreshRequest);
    });

    verify(authService, times(1)).refreshTokenResponse(refreshRequest.getToken());
  }
}
