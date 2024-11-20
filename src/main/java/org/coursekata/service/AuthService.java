package org.coursekata.service;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import org.coursekata.exception.TokenRefreshException;
import org.coursekata.model.auth.AuthenticationResponse;
import org.coursekata.model.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthService {

    private final JwtTokenService jwtTokenService;
    private final TokenStore tokenStore;

    @Autowired
    public AuthService(JwtTokenService jwtTokenService, TokenStore tokenStore) {
        this.jwtTokenService = jwtTokenService;
        this.tokenStore = tokenStore;
    }

    public AuthenticationResponse generateInitialTokenResponse() {
        UUID sessionId = UUID.randomUUID();
        String token = jwtTokenService.generateToken(sessionId.toString());

        logStructuredMessage("Generating initial token for session", sessionId, token);
        tokenStore.storeToken(sessionId, token);

        return createAuthenticationResponse(token);
    }

    public AuthenticationResponse refreshTokenResponse(String token) {
        logStructuredMessage("Refreshing JWT token", null, token);

        UUID sessionId = jwtTokenService.extractSessionIdFromToken(token);
        if (sessionId == null || !isTokenValid(token, sessionId)) {
            throw new TokenRefreshException("Invalid or expired session ID");
        }

        tokenStore.removeToken(sessionId);

        String refreshedToken = jwtTokenService.generateToken(sessionId.toString());
        tokenStore.storeToken(sessionId, refreshedToken);

        logStructuredMessage("Token refreshed successfully", sessionId, refreshedToken);
        return createAuthenticationResponse(refreshedToken);
    }

    private boolean isTokenValid(String token, UUID sessionId) {
        String activeToken = tokenStore.getToken(sessionId);
        return token.equals(activeToken) && jwtTokenService.validateToken(token);
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
