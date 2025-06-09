package org.jupytereverywhere.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;

import org.jupytereverywhere.service.JwtTokenService;

@Log4j2
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

  private static final String MESSAGE_KEY = "Message";
  public static final String ERROR_MESSAGE_KEY = "ErrorMessage";
  public static final String INVALID_OR_EXPIRED_JWT_TOKEN_MESSAGE = "Invalid or expired JWT token";
  public static final String JWT_TOKEN_HAS_EXPIRED_MESSAGE = "JWT Token has expired";

  private final JwtTokenService jwtTokenService;
  private final JwtExtractor jwtExtractor;
  private final JwtValidator jwtValidator;

  
  public JwtRequestFilter(JwtTokenService jwtTokenService, JwtExtractor jwtExtractor, JwtValidator jwtValidator) {
    this.jwtTokenService = jwtTokenService;
    this.jwtExtractor = jwtExtractor;
    this.jwtValidator = jwtValidator;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
      throws ServletException, IOException {
    String jwt = jwtExtractor.extractJwtFromRequest(request);

    if (jwt != null) {
      try {
        if (jwtValidator.isValid(jwt)) {
          UUID sessionId = jwtTokenService.extractSessionIdFromToken(jwt);
          setAuthentication(request, sessionId);
        } else {
          handleInvalidToken(response);
          return;
        }
      } catch (ExpiredJwtException e) {
        handleExpiredToken(response, e);
        return;
      } catch (Exception e) {
        handleTokenProcessingError(response, e);
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private void setAuthentication(HttpServletRequest request, UUID sessionId) {
    log.info(new StringMapMessage().with(MESSAGE_KEY, "Valid session detected").with("sessionId", sessionId.toString()));
    request.setAttribute("sessionId", sessionId);

    Authentication authentication = new UsernamePasswordAuthenticationToken(sessionId, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void handleInvalidToken(HttpServletResponse response) throws IOException {
    log.warn(new StringMapMessage().with(MESSAGE_KEY, INVALID_OR_EXPIRED_JWT_TOKEN_MESSAGE));
    response.sendError(HttpServletResponse.SC_FORBIDDEN, INVALID_OR_EXPIRED_JWT_TOKEN_MESSAGE);
  }

  private void handleExpiredToken(HttpServletResponse response, ExpiredJwtException e) throws IOException {
    log.warn(new StringMapMessage().with(MESSAGE_KEY, JWT_TOKEN_HAS_EXPIRED_MESSAGE).with(
        ERROR_MESSAGE_KEY, e.getMessage()));
    response.sendError(HttpServletResponse.SC_FORBIDDEN, JWT_TOKEN_HAS_EXPIRED_MESSAGE);
  }

  private void handleTokenProcessingError(HttpServletResponse response, Exception e) throws IOException {
    log.error(new StringMapMessage().with(MESSAGE_KEY, "Error during JWT Token processing").with(
        ERROR_MESSAGE_KEY, e.getMessage()), e);
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing JWT token");
  }
}
