package org.jupytereverywhere.controller;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.exception.InvalidNotebookPasswordException;
import org.jupytereverywhere.exception.TokenRefreshException;
import org.jupytereverywhere.model.auth.AuthenticationRequest;
import org.jupytereverywhere.model.auth.AuthenticationResponse;
import org.jupytereverywhere.model.auth.TokenRefreshRequest;
import org.jupytereverywhere.service.AuthService;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String MESSAGE_KEY = "Message";
    private static final String TOKEN_KEY = "Token";

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/issue")
    public ResponseEntity<AuthenticationResponse> issueToken(
        @RequestBody(required = false) AuthenticationRequest authenticationRequest) {
        logInfo("Received token issuance request", "NotebookId",
            authenticationRequest != null ? authenticationRequest.getNotebookId() : "None");

        try {
            AuthenticationResponse authenticationResponse = authService.generateInitialTokenResponse(authenticationRequest);
            HttpHeaders headers = HttpHeaderUtils.createAuthorizationHeader(authenticationResponse.getToken());

            logInfo("Initial token issued successfully", TOKEN_KEY, authenticationResponse.getToken());
            return ResponseEntity.ok().headers(headers).body(authenticationResponse);
        } catch (InvalidNotebookPasswordException e) {
            logError("Invalid notebook ID or password", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new AuthenticationResponse("Invalid notebook ID or password")
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody TokenRefreshRequest refreshRequest) {
        logInfo("Received request to refresh JWT token", TOKEN_KEY, refreshRequest.getToken());

        try {
            AuthenticationResponse authenticationResponse = authService.refreshTokenResponse(refreshRequest.getToken());
            logInfo("Token refreshed successfully", "NewToken", authenticationResponse.getToken());
            return ResponseEntity.ok(authenticationResponse);
        } catch (TokenRefreshException e) {
            logError(refreshRequest.getToken(), e);
            throw e;
        }
    }

    private void logInfo() {
        log.info(new StringMapMessage().with(MESSAGE_KEY, "Issuing initial JWT token for session"));
    }

    private void logInfo(String message, String key, String value) {
        log.info(new StringMapMessage().with(MESSAGE_KEY, message).with(key, value));
    }

    private void logError(String value, Exception e) {
        log.error(new StringMapMessage().with(MESSAGE_KEY, "Error refreshing token").with(
            AuthController.TOKEN_KEY, value), e);
    }
}
