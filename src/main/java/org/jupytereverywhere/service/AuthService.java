package org.jupytereverywhere.service;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.exception.InvalidNotebookPasswordException;
import org.jupytereverywhere.exception.TokenRefreshException;
import org.jupytereverywhere.model.JupyterNotebookEntity;
import org.jupytereverywhere.model.TokenStore;
import org.jupytereverywhere.model.auth.AuthenticationRequest;
import org.jupytereverywhere.model.auth.AuthenticationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthService {

    private final JwtTokenService jwtTokenService;
    private final TokenStore tokenStore;
    private final JupyterNotebookService notebookService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(
        JwtTokenService jwtTokenService,
        TokenStore tokenStore,
        JupyterNotebookService notebookService,
        PasswordEncoder passwordEncoder) {
        this.jwtTokenService = jwtTokenService;
        this.tokenStore = tokenStore;
        this.notebookService = notebookService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticationResponse generateInitialTokenResponse(AuthenticationRequest authRequest) {
        UUID sessionId = UUID.randomUUID();
        String token;

        if (authRequest == null || authRequest.getNotebookId() == null || authRequest.getPassword() == null) {
            token = jwtTokenService.generateToken(sessionId.toString());
        } else {
            if (verifyNotebookPassword(UUID.fromString(authRequest.getNotebookId()), authRequest.getPassword())) {
                token = jwtTokenService.generateToken(sessionId.toString(), authRequest.getNotebookId());
            } else {
                throw new InvalidNotebookPasswordException("Invalid notebook ID or password");
            }
        }

        logStructuredMessage("Generating initial token for session", sessionId, token);
        tokenStore.storeToken(sessionId, token);

        return createAuthenticationResponse(token);
    }

    public AuthenticationResponse refreshTokenResponse(String token) {
        logStructuredMessage("Refreshing JWT token", null, token);

        UUID sessionId = jwtTokenService.extractSessionIdFromToken(token);
        String notebookId = jwtTokenService.extractNotebookIdFromToken(token);
        if (sessionId == null || !isTokenValid(token, sessionId)) {
            throw new TokenRefreshException("Invalid or expired session ID");
        }

        tokenStore.removeToken(sessionId);

        String refreshedToken = jwtTokenService.generateToken(sessionId.toString(), notebookId);
        tokenStore.storeToken(sessionId, refreshedToken);

        logStructuredMessage("Token refreshed successfully", sessionId, refreshedToken);
        return createAuthenticationResponse(refreshedToken);
    }

    private boolean isTokenValid(String token, UUID sessionId) {
        String activeToken = tokenStore.getToken(sessionId);
        return token.equals(activeToken) && jwtTokenService.validateToken(token);
    }

    private boolean verifyNotebookPassword(UUID notebookId, String password) {
        JupyterNotebookEntity notebook = notebookService.getNotebookById(notebookId);
        if (notebook != null && notebook.getPassword() != null) {
            return passwordEncoder.matches(password, notebook.getPassword());
        }
        return false;
    }

    private void logStructuredMessage(String message, UUID sessionId, String token) {
        StringMapMessage logMessage = new StringMapMessage()
            .with("Message", message);
        if (sessionId != null) {
            logMessage.with("SessionId", sessionId.toString());
        }
        if (token != null) {
            logMessage.with("Token", token);
        }
        log.info(logMessage);
    }

    private AuthenticationResponse createAuthenticationResponse(String token) {
        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        return response;
    }
}
