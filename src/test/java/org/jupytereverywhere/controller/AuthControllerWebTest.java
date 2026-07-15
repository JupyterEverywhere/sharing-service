package org.jupytereverywhere.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.exception.AuthExceptionHandler;
import org.jupytereverywhere.exception.TokenRefreshException;
import org.jupytereverywhere.model.auth.AuthenticationRequest;
import org.jupytereverywhere.model.auth.AuthenticationResponse;
import org.jupytereverywhere.service.AuthService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerWebTest {

  @Mock private AuthService authService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new AuthExceptionHandler())
            .build();
  }

  @Test
  void noIssueBodyRemainsAnonymousIssuance() throws Exception {
    when(authService.generateInitialTokenResponse(null))
        .thenReturn(new AuthenticationResponse("anonymous-token"));

    mockMvc
        .perform(post("/auth/issue").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("anonymous-token"));

    verify(authService).generateInitialTokenResponse(null);
  }

  @Test
  void emptyOrPartialIssueBodiesAreRejectedAtHttpBoundary() throws Exception {
    mockMvc
        .perform(post("/auth/issue").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(AuthExceptionHandler.INVALID_REQUEST_MESSAGE));
    mockMvc
        .perform(
            post("/auth/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notebookId\":\"00000000-0000-0000-0000-000000000000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(AuthExceptionHandler.INVALID_REQUEST_MESSAGE));

    verifyNoInteractions(authService);
  }

  @Test
  void oversizedIssueCredentialsAreRejectedWithoutReflection() throws Exception {
    String sentinel = "submitted-password-sentinel-" + "x".repeat(1025);

    mockMvc
        .perform(
            post("/auth/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"notebookId\":\"00000000-0000-0000-0000-000000000000\",\"password\":\""
                        + sentinel
                        + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(AuthExceptionHandler.INVALID_REQUEST_MESSAGE));

    verify(authService, never()).generateInitialTokenResponse(any(AuthenticationRequest.class));
  }

  @Test
  void refreshValidationAndMalformedJsonReturnFixedMessages() throws Exception {
    mockMvc
        .perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(AuthExceptionHandler.INVALID_REQUEST_MESSAGE));
    mockMvc
        .perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(AuthExceptionHandler.MALFORMED_JSON_MESSAGE));

    verifyNoInteractions(authService);
  }

  @Test
  void invalidRefreshUsesGenericForbiddenResponse() throws Exception {
    when(authService.refreshTokenResponse("rejected-token"))
        .thenThrow(new TokenRefreshException("parser detail sentinel"));

    mockMvc
        .perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"rejected-token\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.token").value("Invalid or expired token"));
  }
}
