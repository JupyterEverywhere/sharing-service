package org.coursekata.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.coursekata.exception.InvalidNotebookPasswordException;
import org.coursekata.exception.TokenRefreshException;
import org.coursekata.model.auth.AuthenticationRequest;
import org.coursekata.model.auth.AuthenticationResponse;
import org.coursekata.model.auth.TokenRefreshRequest;
import org.coursekata.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @InjectMocks
  private AuthController authController;

  @Mock
  private AuthService authService;

  @BeforeEach
  public void setUp() {
  }

  @Test
  void testIssueToken_Success_WithAuthenticationRequest() {
    AuthenticationRequest authenticationRequest = new AuthenticationRequest();
    authenticationRequest.setNotebookId("notebook-id");
    authenticationRequest.setPassword("password");

    AuthenticationResponse expectedResponse = new AuthenticationResponse("generated-token");

    when(authService.generateInitialTokenResponse(authenticationRequest)).thenReturn(expectedResponse);

    ResponseEntity<AuthenticationResponse> responseEntity = authController.issueToken(authenticationRequest);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    HttpHeaders headers = responseEntity.getHeaders();
    assertTrue(headers.containsKey("Authorization"));
    assertEquals("Bearer generated-token", headers.getFirst("Authorization"));

    AuthenticationResponse responseBody = responseEntity.getBody();
    assertNotNull(responseBody);
    assertEquals("generated-token", responseBody.getToken());

    verify(authService, times(1)).generateInitialTokenResponse(authenticationRequest);
  }

  @Test
  void testIssueToken_Success_WithoutAuthenticationRequest() {
    AuthenticationRequest authenticationRequest = null;

    AuthenticationResponse expectedResponse = new AuthenticationResponse("generated-token");

    when(authService.generateInitialTokenResponse(null)).thenReturn(expectedResponse);

    ResponseEntity<AuthenticationResponse> responseEntity = authController.issueToken(authenticationRequest);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    HttpHeaders headers = responseEntity.getHeaders();
    assertTrue(headers.containsKey("Authorization"));
    assertEquals("Bearer generated-token", headers.getFirst("Authorization"));

    AuthenticationResponse responseBody = responseEntity.getBody();
    assertNotNull(responseBody);
    assertEquals("generated-token", responseBody.getToken());

    verify(authService, times(1)).generateInitialTokenResponse(null);
  }

  @Test
  void testIssueToken_InvalidNotebookPasswordException() {
    AuthenticationRequest authenticationRequest = new AuthenticationRequest();
    authenticationRequest.setNotebookId("notebook-id");
    authenticationRequest.setPassword("invalid-password");

    when(authService.generateInitialTokenResponse(authenticationRequest))
        .thenThrow(new InvalidNotebookPasswordException("Invalid notebook ID or password"));

    ResponseEntity<AuthenticationResponse> responseEntity = authController.issueToken(authenticationRequest);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

    AuthenticationResponse responseBody = responseEntity.getBody();
    assertNotNull(responseBody);
    assertEquals("Invalid notebook ID or password", responseBody.getToken());

    verify(authService, times(1)).generateInitialTokenResponse(authenticationRequest);
  }

  @Test
  void testIssueToken_UnexpectedException() {
    AuthenticationRequest authenticationRequest = new AuthenticationRequest();
    authenticationRequest.setNotebookId("notebook-id");
    authenticationRequest.setPassword("password");

    when(authService.generateInitialTokenResponse(authenticationRequest))
        .thenThrow(new RuntimeException("Unexpected error"));

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authController.issueToken(authenticationRequest);
    });

    assertEquals("Unexpected error", exception.getMessage());

    verify(authService, times(1)).generateInitialTokenResponse(authenticationRequest);
  }

  @Test
  void testRefreshToken_Success() {
    TokenRefreshRequest refreshRequest = new TokenRefreshRequest("valid-refresh-token");
    AuthenticationResponse expectedResponse = new AuthenticationResponse("refreshed-token");

    when(authService.refreshTokenResponse(refreshRequest.getToken())).thenReturn(expectedResponse);

    ResponseEntity<AuthenticationResponse> responseEntity = authController.refreshToken(refreshRequest);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    AuthenticationResponse responseBody = responseEntity.getBody();
    assertNotNull(responseBody);
    assertEquals("refreshed-token", responseBody.getToken());

    verify(authService, times(1)).refreshTokenResponse(refreshRequest.getToken());
  }

  @Test
  void testRefreshToken_TokenRefreshException() {
    TokenRefreshRequest refreshRequest = new TokenRefreshRequest("invalid-refresh-token");

    when(authService.refreshTokenResponse(refreshRequest.getToken()))
        .thenThrow(new TokenRefreshException("Token refresh failed"));

    TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> {
      authController.refreshToken(refreshRequest);
    });

    assertEquals("Token refresh failed", exception.getMessage());

    verify(authService, times(1)).refreshTokenResponse(refreshRequest.getToken());
  }

  @Test
  void testRefreshToken_UnexpectedException() {
    TokenRefreshRequest refreshRequest = new TokenRefreshRequest("valid-refresh-token");

    when(authService.refreshTokenResponse(refreshRequest.getToken()))
        .thenThrow(new RuntimeException("Unexpected error"));

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authController.refreshToken(refreshRequest);
    });

    assertEquals("Unexpected error", exception.getMessage());

    verify(authService, times(1)).refreshTokenResponse(refreshRequest.getToken());
  }
}
