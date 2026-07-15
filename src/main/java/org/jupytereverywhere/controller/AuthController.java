package org.jupytereverywhere.controller;

import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.exception.InvalidNotebookPasswordException;
import org.jupytereverywhere.exception.TokenRefreshException;
import org.jupytereverywhere.model.auth.AdminTokenRequest;
import org.jupytereverywhere.model.auth.AuthenticationRequest;
import org.jupytereverywhere.model.auth.AuthenticationResponse;
import org.jupytereverywhere.model.auth.TokenRefreshRequest;
import org.jupytereverywhere.service.AuthService;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/auth")
public class AuthController {

  private static final String MESSAGE_KEY = "Message";

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/admin/token")
  public ResponseEntity<AuthenticationResponse> issueAdminToken(
      @Valid @RequestBody AdminTokenRequest adminTokenRequest) {
    logInfo("Received admin token request", "TokenName", adminTokenRequest.getTokenName());

    AuthenticationResponse response = authService.generateAdminTokenResponse(adminTokenRequest);
    if (response == null) {
      logInfo("Admin token request rejected", "TokenName", adminTokenRequest.getTokenName());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new AuthenticationResponse("Invalid admin secret"));
    }

    logInfo("Admin token issued successfully", "TokenName", adminTokenRequest.getTokenName());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/issue")
  public ResponseEntity<AuthenticationResponse> issueToken(
      @Valid @RequestBody(required = false) AuthenticationRequest authenticationRequest) {
    logInfo("Received token issuance request");

    try {
      AuthenticationResponse authenticationResponse =
          authService.generateInitialTokenResponse(authenticationRequest);
      HttpHeaders headers =
          HttpHeaderUtils.createAuthorizationHeader(authenticationResponse.getToken());

      logInfo("Initial token issued successfully");
      return ResponseEntity.ok().headers(headers).body(authenticationResponse);
    } catch (InvalidNotebookPasswordException e) {
      logError("Initial token request rejected");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new AuthenticationResponse("Invalid notebook ID or password"));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthenticationResponse> refreshToken(
      @Valid @RequestBody TokenRefreshRequest refreshRequest) {
    logInfo("Received request to refresh JWT token");

    try {
      AuthenticationResponse authenticationResponse =
          authService.refreshTokenResponse(refreshRequest.getToken());
      logInfo("Token refreshed successfully");
      return ResponseEntity.ok(authenticationResponse);
    } catch (TokenRefreshException e) {
      logError("Token refresh rejected");
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new AuthenticationResponse("Invalid or expired token"));
    }
  }

  private void logInfo(String message) {
    log.info(new StringMapMessage().with(MESSAGE_KEY, message));
  }

  private void logInfo(String message, String key, String value) {
    log.info(new StringMapMessage().with(MESSAGE_KEY, message).with(key, value));
  }

  private void logError(String message) {
    log.warn(new StringMapMessage().with(MESSAGE_KEY, message));
  }
}
