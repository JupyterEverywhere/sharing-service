package org.jupytereverywhere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.exception.TokenRefreshException;
import org.jupytereverywhere.model.TokenStore;
import org.jupytereverywhere.model.auth.AdminTokenRequest;
import org.jupytereverywhere.model.auth.AuthenticationRequest;
import org.jupytereverywhere.model.auth.AuthenticationResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

  @Mock private JwtTokenService jwtTokenService;

  @Mock private TokenStore tokenStore;

  @InjectMocks private AuthService authService;

  @Test
  void testGenerateInitialTokenResponse_Success(CapturedOutput output) {
    String expectedToken = "issued-token-sentinel";
    when(jwtTokenService.generateToken(anyString())).thenReturn(expectedToken);

    AuthenticationResponse response =
        authService.generateInitialTokenResponse(new AuthenticationRequest());

    assertNotNull(response);
    assertEquals(expectedToken, response.getToken());
    assertFalse(output.getAll().contains(expectedToken));
    verify(jwtTokenService).generateToken(anyString());
    verify(tokenStore).storeToken(any(UUID.class), eq(expectedToken));
  }

  @Test
  void testRefreshTokenResponse_Success(CapturedOutput output) {
    String oldToken = "old-token-sentinel";
    UUID sessionId = UUID.randomUUID();
    String expectedToken = "new-token-sentinel";
    String notebookId = "notebook-123";

    when(jwtTokenService.extractSessionIdFromToken(oldToken)).thenReturn(sessionId);
    when(jwtTokenService.extractNotebookIdFromToken(oldToken)).thenReturn(notebookId);
    when(tokenStore.getToken(sessionId)).thenReturn(oldToken);
    when(jwtTokenService.validateToken(oldToken)).thenReturn(true);
    when(jwtTokenService.generateToken(sessionId.toString(), notebookId)).thenReturn(expectedToken);

    AuthenticationResponse response = authService.refreshTokenResponse(oldToken);

    assertNotNull(response, "AuthenticationResponse should not be null");
    assertEquals(
        expectedToken, response.getToken(), "The new token should match the expected value");
    assertFalse(output.getAll().contains(oldToken));
    assertFalse(output.getAll().contains(expectedToken));

    verify(jwtTokenService).extractSessionIdFromToken(oldToken);
    verify(jwtTokenService).extractNotebookIdFromToken(oldToken);
    verify(jwtTokenService).validateToken(oldToken);
    verify(jwtTokenService).generateToken(sessionId.toString(), notebookId);
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
  void testGenerateAdminTokenResponse_Success(CapturedOutput output) {
    ReflectionTestUtils.setField(authService, "adminSecret", "test-secret");
    String expectedToken = "admin-token-sentinel";
    when(jwtTokenService.generateAdminToken(anyString(), eq("ops-team-1")))
        .thenReturn(expectedToken);

    AdminTokenRequest request = new AdminTokenRequest("test-secret", "ops-team-1");
    AuthenticationResponse response = authService.generateAdminTokenResponse(request);

    assertNotNull(response);
    assertEquals(expectedToken, response.getToken());
    assertFalse(output.getAll().contains(expectedToken));
    verify(jwtTokenService).generateAdminToken(anyString(), eq("ops-team-1"));
    verify(tokenStore).storeToken(any(UUID.class), eq(expectedToken));
  }

  @Test
  void testGenerateAdminTokenResponse_InvalidSecret() {
    ReflectionTestUtils.setField(authService, "adminSecret", "test-secret");

    AdminTokenRequest request = new AdminTokenRequest("wrong-secret", "ops-team-1");
    AuthenticationResponse response = authService.generateAdminTokenResponse(request);

    assertNull(response);
    verify(jwtTokenService, never()).generateAdminToken(anyString(), anyString());
    verify(tokenStore, never()).storeToken(any(UUID.class), anyString());
  }

  @Test
  void testGenerateAdminTokenResponse_EmptyAdminSecret() {
    ReflectionTestUtils.setField(authService, "adminSecret", "");

    AdminTokenRequest request = new AdminTokenRequest("any-secret", "ops-team-1");
    AuthenticationResponse response = authService.generateAdminTokenResponse(request);

    assertNull(response);
  }

  @Test
  void testRefreshTokenResponse_AdminToken_PreservesClaimsOnRefresh() {
    String oldToken = "old-admin-token";
    UUID sessionId = UUID.randomUUID();
    String expectedToken = "new-admin-token";

    when(jwtTokenService.extractSessionIdFromToken(oldToken)).thenReturn(sessionId);
    when(jwtTokenService.extractNotebookIdFromToken(oldToken)).thenReturn(null);
    when(jwtTokenService.extractRoleFromToken(oldToken)).thenReturn("ADMIN");
    when(jwtTokenService.extractTokenNameFromToken(oldToken)).thenReturn("ops-team-1");
    when(tokenStore.getToken(sessionId)).thenReturn(oldToken);
    when(jwtTokenService.validateToken(oldToken)).thenReturn(true);
    when(jwtTokenService.generateAdminToken(sessionId.toString(), "ops-team-1"))
        .thenReturn(expectedToken);

    AuthenticationResponse response = authService.refreshTokenResponse(oldToken);

    assertNotNull(response);
    assertEquals(expectedToken, response.getToken());
    verify(jwtTokenService).generateAdminToken(sessionId.toString(), "ops-team-1");
    verify(jwtTokenService, never()).generateToken(anyString(), anyString());
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
