package org.coursekata.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.coursekata.dto.JupyterNotebookDTO;
import org.coursekata.exception.InvalidNotebookException;
import org.coursekata.exception.NotebookNotFoundException;
import org.coursekata.exception.SessionMismatchException;
import org.coursekata.model.request.JupyterNotebookRequest;
import org.coursekata.model.response.JupyterNotebookErrorResponse;
import org.coursekata.model.response.JupyterNotebookResponse;
import org.coursekata.model.response.JupyterNotebookRetrieved;
import org.coursekata.model.response.JupyterNotebookSaved;
import org.coursekata.model.response.JupyterNotebookSavedResponse;
import org.coursekata.service.JupyterNotebookService;
import org.coursekata.utils.HttpHeaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

class JupyterNotebookControllerTest {

  @Mock
  private JupyterNotebookService notebookService;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private JupyterNotebookController controller;

  @Mock
  private HttpServletRequest request;

  private final String domain = "example.com";
  private final String readableId = "adorable-amazing-alligator";

  @BeforeEach
  void setUp() {
    openMocks(this);
  }

  @Test
  void testGetNotebook_Success() {
    UUID notebookId = UUID.randomUUID();

    var notebookRetrieved = new JupyterNotebookRetrieved(notebookId, domain, readableId, new JupyterNotebookDTO());

    when(notebookService.getNotebookContent(notebookId)).thenReturn(notebookRetrieved);

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(notebookId);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(notebookRetrieved, response.getBody());
  }

  @Test
  void testGetNotebook_NotFound() {
    UUID notebookId = UUID.randomUUID();
    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.NOT_FOUND.name(), "Notebook not found");

    doThrow(new NotebookNotFoundException("Notebook not found"))
        .when(notebookService).getNotebookContent(notebookId);

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(notebookId);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(404, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }

  @Test
  void testGetNotebook_Exception() {
    UUID notebookId = UUID.randomUUID();
    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), "Error fetching notebook");

    doThrow(new RuntimeException("Unexpected error"))
        .when(notebookService).getNotebookContent(notebookId);

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(notebookId);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(500, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }

  @Test
  void testUploadNotebook_Success() {
    JupyterNotebookRequest jupyterNotebookRequest = new JupyterNotebookRequest();
    UUID sessionId = UUID.randomUUID();
    String message = "Notebook uploaded, validated, and metadata stored successfully";

    UUID notebookId = UUID.randomUUID();
    var notebookSaved = new JupyterNotebookSaved(notebookId, domain, readableId);
    var notebookSavedResponse = new JupyterNotebookSavedResponse(message, notebookSaved);


    when(authentication.getPrincipal()).thenReturn(sessionId);
    when(request.getRemoteAddr()).thenReturn(domain);

    when(notebookService.uploadNotebook(jupyterNotebookRequest, sessionId, domain)).thenReturn(notebookSaved);

    ResponseEntity<JupyterNotebookResponse> response = controller.uploadNotebook(jupyterNotebookRequest, authentication, request);

    assertEquals(201, response.getStatusCode().value());
    assertEquals(notebookSavedResponse, response.getBody());
  }

  @Test
  void testUploadNotebook_InvalidNotebook() {
    JupyterNotebookRequest jupyterNotebookRequest = new JupyterNotebookRequest();
    UUID sessionId = UUID.randomUUID();
    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.name(), "Invalid notebook format");

    when(authentication.getPrincipal()).thenReturn(sessionId);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(HttpHeaderUtils.getDomainFromRequest(request)).thenReturn(domain);

    doThrow(new InvalidNotebookException("Invalid notebook format"))
        .when(notebookService).uploadNotebook(jupyterNotebookRequest, sessionId, domain);

    ResponseEntity<JupyterNotebookResponse> response = controller.uploadNotebook(jupyterNotebookRequest, authentication, request);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(422, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }

  @Test
  void testUploadNotebook_Exception() {
    JupyterNotebookRequest jupyterNotebookRequest = new JupyterNotebookRequest();
    UUID sessionId = UUID.randomUUID();
    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), "Error uploading notebook");

    when(authentication.getPrincipal()).thenReturn(sessionId);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(HttpHeaderUtils.getDomainFromRequest(request)).thenReturn(domain);

    doThrow(new RuntimeException("Unexpected error"))
        .when(notebookService).uploadNotebook(eq(jupyterNotebookRequest), eq(sessionId), eq(domain));

    ResponseEntity<JupyterNotebookResponse> response = controller.uploadNotebook(jupyterNotebookRequest, authentication, request);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(500, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }

  @Test
  void testUpdateNotebook_Success() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();

    String message = "Notebook updated successfully";
    var notebookSaved = new JupyterNotebookSaved(notebookId, domain, readableId);
    var notebookSavedResponse = new JupyterNotebookSavedResponse(message, notebookSaved);

    when(authentication.getPrincipal()).thenReturn(sessionId);
    when(notebookService.updateNotebook(notebookId, notebookDto, sessionId)).thenReturn(notebookSaved);

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(notebookSavedResponse, response.getBody());
  }

  @Test
  void testUpdateNotebook_SessionMismatch() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();

    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.UNAUTHORIZED.name(), "Session ID mismatch");

    when(authentication.getPrincipal()).thenReturn(sessionId);
    doThrow(new SessionMismatchException("Session ID mismatch"))
        .when(notebookService).updateNotebook(notebookId, notebookDto, sessionId);

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(401, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }

  @Test
  void testUpdateNotebook_InvalidNotebook() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();

    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.BAD_REQUEST.name(), "Invalid notebook format");

    when(authentication.getPrincipal()).thenReturn(sessionId);
    doThrow(new InvalidNotebookException("Invalid notebook format"))
        .when(notebookService).updateNotebook(notebookId, notebookDto, sessionId);

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(400, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }

  @Test
  void testUpdateNotebook_Exception() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();

    var errorResponse = new JupyterNotebookErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), "Error updating notebook");

    when(authentication.getPrincipal()).thenReturn(sessionId);
    doThrow(new RuntimeException("Unexpected error"))
        .when(notebookService).updateNotebook(notebookId, notebookDto, sessionId);

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication);
    JupyterNotebookErrorResponse notebookResponse = (JupyterNotebookErrorResponse) response.getBody();

    assertNotNull(notebookResponse);
    assertEquals(500, response.getStatusCode().value());
    assertEquals(errorResponse.getErrorCode(), notebookResponse.getErrorCode());
    assertEquals(errorResponse.getMessage(), notebookResponse.getMessage());
  }
}
