package org.jupytereverywhere.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.controller.JupyterNotebookController;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.exception.InvalidNotebookException;
import org.jupytereverywhere.exception.InvalidNotebookPasswordException;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.exception.SessionMismatchException;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookErrorResponse;
import org.jupytereverywhere.model.response.JupyterNotebookResponse;
import org.jupytereverywhere.model.response.JupyterNotebookRetrieved;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.jupytereverywhere.model.response.JupyterNotebookSavedResponse;
import org.jupytereverywhere.service.JupyterNotebookService;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class JupyterNotebookControllerTest {

  @InjectMocks
  private JupyterNotebookController controller;

  @Mock
  private JupyterNotebookService notebookService;

  @Mock
  private Authentication authentication;

  @Mock
  private HttpServletRequest request;

  private final String domain = "example.com";
  private final String readableId = "adorable-amazing-alligator";

  private MockedStatic<HttpHeaderUtils> mockedStaticHttpHeaderUtils;

  @BeforeEach
  void setUp() {
    mockedStaticHttpHeaderUtils = mockStatic(HttpHeaderUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedStaticHttpHeaderUtils.close();
  }

  private void mockDomainExtraction() {
    mockedStaticHttpHeaderUtils.when(() -> HttpHeaderUtils.getDomainFromRequest(request)).thenReturn(
        "example.com");
  }

  private void mockTokenExtraction(String token) {
    mockedStaticHttpHeaderUtils.when(() -> HttpHeaderUtils.getTokenFromRequest(request)).thenReturn(token);
  }


  @Test
  void testGetNotebookById_Success() {
    UUID notebookId = UUID.randomUUID();
    var notebookRetrieved = new JupyterNotebookRetrieved(notebookId, domain, readableId, new JupyterNotebookDTO());

    when(notebookService.getNotebookContent(notebookId)).thenReturn(notebookRetrieved);

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(notebookId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(notebookRetrieved, response.getBody());
  }

  @Test
  void testGetNotebookById_NotFound() {
    UUID notebookId = UUID.randomUUID();

    when(notebookService.getNotebookContent(notebookId))
        .thenThrow(new NotebookNotFoundException("Notebook not found"));

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(notebookId);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Notebook not found", errorResponse.getMessage());
    assertEquals(HttpStatus.NOT_FOUND.name(), errorResponse.getErrorCode());
  }

  @Test
  void testGetNotebookById_Exception() {
    UUID notebookId = UUID.randomUUID();

    when(notebookService.getNotebookContent(notebookId))
        .thenThrow(new RuntimeException("Unexpected error"));

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(notebookId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Error fetching notebook", errorResponse.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.name(), errorResponse.getErrorCode());
  }

  @Test
  void testGetNotebookByReadableId_Success() {
    var notebookRetrieved = new JupyterNotebookRetrieved(UUID.randomUUID(), domain, readableId, new JupyterNotebookDTO());

    when(notebookService.getNotebookContent(readableId)).thenReturn(notebookRetrieved);

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(readableId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(notebookRetrieved, response.getBody());
  }

  @Test
  void testGetNotebookByReadableId_NotFound() {
    when(notebookService.getNotebookContent(readableId))
        .thenThrow(new NotebookNotFoundException("Notebook not found"));

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(readableId);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Notebook not found", errorResponse.getMessage());
    assertEquals(HttpStatus.NOT_FOUND.name(), errorResponse.getErrorCode());
  }

  @Test
  void testGetNotebookByReadableId_Exception() {
    when(notebookService.getNotebookContent(readableId))
        .thenThrow(new RuntimeException("Unexpected error"));

    ResponseEntity<JupyterNotebookResponse> response = controller.getNotebook(readableId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Error fetching notebook", errorResponse.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUploadNotebook_Success() {
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    UUID sessionId = UUID.randomUUID();
    UUID notebookId = UUID.randomUUID();
    String message = "Notebook uploaded, validated, and metadata stored successfully";

    var notebookSaved = new JupyterNotebookSaved(notebookId, domain, readableId);
    var expectedResponse = new JupyterNotebookSavedResponse(message, notebookSaved);

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockDomainExtraction();
    when(notebookService.uploadNotebook(notebookRequest, sessionId, domain)).thenReturn(notebookSaved);

    ResponseEntity<JupyterNotebookResponse> response = controller.uploadNotebook(notebookRequest, authentication, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
  }

  @Test
  void testUploadNotebook_InvalidNotebookException() {
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    UUID sessionId = UUID.randomUUID();

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockDomainExtraction();
    when(notebookService.uploadNotebook(notebookRequest, sessionId, domain))
        .thenThrow(new InvalidNotebookException("Invalid notebook format"));

    ResponseEntity<JupyterNotebookResponse> response = controller.uploadNotebook(notebookRequest, authentication, request);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Invalid notebook format", errorResponse.getMessage());
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUploadNotebook_Exception() {
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    UUID sessionId = UUID.randomUUID();

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockDomainExtraction();
    when(notebookService.uploadNotebook(notebookRequest, sessionId, domain))
        .thenThrow(new RuntimeException("Unexpected error"));

    ResponseEntity<JupyterNotebookResponse> response = controller.uploadNotebook(notebookRequest, authentication, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Error uploading notebook", errorResponse.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookById_Success() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";
    String message = "Notebook updated successfully";

    var notebookSaved = new JupyterNotebookSaved(notebookId, domain, readableId);
    var expectedResponse = new JupyterNotebookSavedResponse(message, notebookSaved);

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(notebookId, notebookDto, sessionId, token)).thenReturn(notebookSaved);

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
  }

  @Test
  void testUpdateNotebookById_InvalidPasswordException() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "invalid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(notebookId, notebookDto, sessionId, token))
        .thenThrow(new InvalidNotebookPasswordException("Invalid password"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Invalid password", errorResponse.getMessage());
    assertEquals(HttpStatus.UNAUTHORIZED.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookById_SessionMismatchException() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(notebookId, notebookDto, sessionId, token))
        .thenThrow(new SessionMismatchException("Session ID mismatch"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Session ID mismatch", errorResponse.getMessage());
    assertEquals(HttpStatus.UNAUTHORIZED.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookById_InvalidNotebookException() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(notebookId, notebookDto, sessionId, token))
        .thenThrow(new InvalidNotebookException("Invalid notebook format"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Invalid notebook format", errorResponse.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookById_Exception() throws JsonProcessingException {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(notebookId, notebookDto, sessionId, token))
        .thenThrow(new RuntimeException("Unexpected error"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(notebookId, notebookDto, authentication, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Error updating notebook", errorResponse.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookByReadableId_Success() throws JsonProcessingException {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";
    String message = "Notebook updated successfully";

    var notebookSaved = new JupyterNotebookSaved(UUID.randomUUID(), domain, readableId);
    var expectedResponse = new JupyterNotebookSavedResponse(message, notebookSaved);

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(readableId, notebookDto, sessionId, token))
        .thenReturn(notebookSaved);

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(
        readableId, notebookDto, authentication, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
  }

  @Test
  void testUpdateNotebookByReadableId_SessionMismatchException() throws JsonProcessingException {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(readableId, notebookDto, sessionId, token))
        .thenThrow(new SessionMismatchException("Session ID mismatch"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(
        readableId, notebookDto, authentication, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Session ID mismatch", errorResponse.getMessage());
    assertEquals(HttpStatus.UNAUTHORIZED.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookByReadableId_InvalidNotebookException() throws JsonProcessingException {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(readableId, notebookDto, sessionId, token))
        .thenThrow(new InvalidNotebookException("Invalid notebook format"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(
        readableId, notebookDto, authentication, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Invalid notebook format", errorResponse.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST.name(), errorResponse.getErrorCode());
  }

  @Test
  void testUpdateNotebookByReadableId_Exception() throws JsonProcessingException {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    UUID sessionId = UUID.randomUUID();
    String token = "valid-token";

    when(authentication.getPrincipal()).thenReturn(sessionId);
    mockTokenExtraction(token);
    when(notebookService.updateNotebook(readableId, notebookDto, sessionId, token))
        .thenThrow(new RuntimeException("Unexpected error"));

    ResponseEntity<JupyterNotebookResponse> response = controller.updateNotebook(
        readableId, notebookDto, authentication, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    JupyterNotebookErrorResponse errorResponse = (JupyterNotebookErrorResponse) response.getBody();
    assertNotNull(errorResponse);
    assertEquals("Error updating notebook", errorResponse.getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.name(), errorResponse.getErrorCode());
  }

}
