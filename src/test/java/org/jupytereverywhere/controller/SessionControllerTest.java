package org.jupytereverywhere.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.dto.SessionDeleteResponse;
import org.jupytereverywhere.service.JupyterNotebookService;
import org.jupytereverywhere.service.JwtTokenService;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

  @InjectMocks private SessionController controller;

  @Mock private JupyterNotebookService notebookService;

  @Mock private JwtTokenService jwtTokenService;

  @Mock private HttpServletRequest request;

  private MockedStatic<HttpHeaderUtils> mockedStaticHttpHeaderUtils;

  @BeforeEach
  void setUp() {
    mockedStaticHttpHeaderUtils = mockStatic(HttpHeaderUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedStaticHttpHeaderUtils.close();
  }

  private void mockTokenExtraction(String token) {
    mockedStaticHttpHeaderUtils
        .when(() -> HttpHeaderUtils.getTokenFromRequest(request))
        .thenReturn(token);
    mockedStaticHttpHeaderUtils
        .when(() -> HttpHeaderUtils.extractAdminTokenName(request, jwtTokenService))
        .thenCallRealMethod();
  }

  @Test
  void testDeleteNotebooksBySession_Success() {
    UUID sessionId = UUID.randomUUID();
    mockTokenExtraction("admin-token");
    when(jwtTokenService.extractTokenNameFromToken("admin-token")).thenReturn("ops-team-1");
    when(notebookService.deleteNotebooksBySessionId(sessionId, "ops-team-1")).thenReturn(3);

    ResponseEntity<?> response = controller.deleteNotebooksBySession(sessionId, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(3, ((SessionDeleteResponse) response.getBody()).deletedCount());
  }

  @Test
  void testDeleteNotebooksBySession_ZeroNotebooks() {
    UUID sessionId = UUID.randomUUID();
    mockTokenExtraction("admin-token");
    when(jwtTokenService.extractTokenNameFromToken("admin-token")).thenReturn("ops-team-1");
    when(notebookService.deleteNotebooksBySessionId(sessionId, "ops-team-1")).thenReturn(0);

    ResponseEntity<?> response = controller.deleteNotebooksBySession(sessionId, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, ((SessionDeleteResponse) response.getBody()).deletedCount());
  }

  @Test
  void testDeleteNotebooksBySession_InternalError() {
    UUID sessionId = UUID.randomUUID();
    mockTokenExtraction("admin-token");
    when(jwtTokenService.extractTokenNameFromToken("admin-token")).thenReturn("ops-team-1");
    when(notebookService.deleteNotebooksBySessionId(sessionId, "ops-team-1"))
        .thenThrow(new RuntimeException("Storage failure"));

    ResponseEntity<?> response = controller.deleteNotebooksBySession(sessionId, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }
}
