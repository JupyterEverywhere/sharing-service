package org.jupytereverywhere.controller;

import java.util.UUID;

import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.exception.InvalidNotebookException;
import org.jupytereverywhere.exception.InvalidNotebookPasswordException;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.exception.NotebookTooLargeException;
import org.jupytereverywhere.exception.SessionMismatchException;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookErrorResponse;
import org.jupytereverywhere.model.response.JupyterNotebookResponse;
import org.jupytereverywhere.model.response.JupyterNotebookRetrieved;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.jupytereverywhere.model.response.JupyterNotebookSavedResponse;
import org.jupytereverywhere.service.JupyterNotebookService;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/notebooks")
public class JupyterNotebookController {

  private static final String MESSAGE_KEY = "Message";
  private static final String SESSION_ID_MESSAGE_KEY = "SessionID";
  private static final String NOTEBOOK_ID_MESSAGE_KEY = "NotebookID";
  private static final String READABLE_ID_MESSAGE_KEY = "ReadableID";

  private final JupyterNotebookService notebookService;

  public JupyterNotebookController(JupyterNotebookService notebookService) {
    this.notebookService = notebookService;
  }

  @GetMapping("/{uuid}")
  public ResponseEntity<JupyterNotebookResponse> getNotebook(@PathVariable UUID uuid) {
    logInfo("Received request to fetch notebook", NOTEBOOK_ID_MESSAGE_KEY, uuid.toString());
    try {
      JupyterNotebookRetrieved notebookRetrieved = notebookService.getNotebookContent(uuid);
      return ResponseEntity.ok(notebookRetrieved);
    } catch (NotebookNotFoundException e) {
      return handleException(HttpStatus.NOT_FOUND, "Notebook not found", e, uuid);
    } catch (Exception e) {
      return handleException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching notebook", e, uuid);
    }
  }

  @GetMapping("/get-by-readable-id/{readableId}")
  public ResponseEntity<JupyterNotebookResponse> getNotebook(@PathVariable String readableId) {
    logInfo("Received request to fetch notebook", READABLE_ID_MESSAGE_KEY, readableId);
    try {
      JupyterNotebookRetrieved notebookContent = notebookService.getNotebookContent(readableId);
      return ResponseEntity.ok(notebookContent);
    } catch (NotebookNotFoundException e) {
      return handleException(HttpStatus.NOT_FOUND, "Notebook not found", e, readableId);
    } catch (Exception e) {
      return handleException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching notebook", e, readableId);
    }
  }

  @PostMapping
  public ResponseEntity<JupyterNotebookResponse> uploadNotebook(
      @Valid @RequestBody JupyterNotebookRequest notebookRequest,
      Authentication authentication,
      HttpServletRequest request) {

    UUID sessionId = (UUID) authentication.getPrincipal();
    String domain = HttpHeaderUtils.getDomainFromRequest(request);

    logInfo("Received notebook upload request", SESSION_ID_MESSAGE_KEY, sessionId.toString());

    try {
      JupyterNotebookSaved notebookSaved =
          notebookService.uploadNotebook(notebookRequest, sessionId, domain);
      logInfo(
          "Notebook uploaded and validated successfully",
          SESSION_ID_MESSAGE_KEY,
          sessionId.toString());

      var response =
          new JupyterNotebookSavedResponse(
              "Notebook uploaded, validated, and metadata stored successfully", notebookSaved);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (NotebookTooLargeException e) {
      return handleSizeLimitException(e, sessionId);
    } catch (InvalidNotebookException e) {
      return handleException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Invalid notebook format", e, sessionId);
    } catch (Exception e) {
      return handleException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading notebook", e, sessionId);
    }
  }

  @PutMapping("/{uuid}")
  public ResponseEntity<JupyterNotebookResponse> updateNotebook(
      @PathVariable UUID uuid,
      @Valid @RequestBody JupyterNotebookDTO notebookDto,
      Authentication authentication,
      HttpServletRequest request) {

    UUID sessionId = (UUID) authentication.getPrincipal();

    logInfo(
        "Received notebook update request",
        NOTEBOOK_ID_MESSAGE_KEY,
        uuid.toString(),
        SESSION_ID_MESSAGE_KEY,
        sessionId.toString());

    try {
      String token = HttpHeaderUtils.getTokenFromRequest(request);

      JupyterNotebookSaved notebookUpdated =
          notebookService.updateNotebook(uuid, notebookDto, sessionId, token);

      logInfo(
          "Notebook updated successfully",
          NOTEBOOK_ID_MESSAGE_KEY,
          uuid.toString(),
          SESSION_ID_MESSAGE_KEY,
          sessionId.toString());

      var response =
          new JupyterNotebookSavedResponse("Notebook updated successfully", notebookUpdated);
      return ResponseEntity.ok(response);
    } catch (NotebookTooLargeException e) {
      return handleSizeLimitException(
          e, NOTEBOOK_ID_MESSAGE_KEY, uuid, SESSION_ID_MESSAGE_KEY, sessionId);
    } catch (InvalidNotebookPasswordException e) {
      return handleException(HttpStatus.UNAUTHORIZED, "Invalid password", e, uuid, sessionId);
    } catch (SessionMismatchException e) {
      return handleException(HttpStatus.UNAUTHORIZED, "Session ID mismatch", e, uuid, sessionId);
    } catch (InvalidNotebookException e) {
      return handleException(HttpStatus.BAD_REQUEST, "Invalid notebook format", e, uuid, sessionId);
    } catch (Exception e) {
      return handleException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error updating notebook", e, uuid, sessionId);
    }
  }

  @PutMapping("/update-by-readable-id/{readableId}")
  public ResponseEntity<JupyterNotebookResponse> updateNotebook(
      @PathVariable String readableId,
      @Valid @RequestBody JupyterNotebookDTO notebookDto,
      Authentication authentication,
      HttpServletRequest request) {
    UUID sessionId = (UUID) authentication.getPrincipal();
    logInfo(
        "Received notebook update request",
        READABLE_ID_MESSAGE_KEY,
        readableId,
        SESSION_ID_MESSAGE_KEY,
        sessionId.toString());

    try {
      String token = HttpHeaderUtils.getTokenFromRequest(request);

      JupyterNotebookSaved notebookUpdated =
          notebookService.updateNotebook(readableId, notebookDto, sessionId, token);
      logInfo(
          "Notebook updated successfully",
          NOTEBOOK_ID_MESSAGE_KEY,
          readableId,
          SESSION_ID_MESSAGE_KEY,
          sessionId.toString());

      var response =
          new JupyterNotebookSavedResponse("Notebook updated successfully", notebookUpdated);
      return ResponseEntity.ok(response);
    } catch (NotebookTooLargeException e) {
      return handleSizeLimitException(
          e, READABLE_ID_MESSAGE_KEY, readableId, SESSION_ID_MESSAGE_KEY, sessionId);
    } catch (InvalidNotebookException e) {
      return handleException(
          HttpStatus.BAD_REQUEST, "Invalid notebook format", e, readableId, sessionId);
    } catch (SessionMismatchException e) {
      return handleException(
          HttpStatus.UNAUTHORIZED, "Session ID mismatch", e, readableId, sessionId);
    } catch (Exception e) {
      return handleException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error updating notebook", e, readableId, sessionId);
    }
  }

  private void logInfo(String message, String... params) {
    StringMapMessage logMessage = new StringMapMessage().with(MESSAGE_KEY, message);
    for (int i = 0; i < params.length; i += 2) {
      if (i + 1 < params.length) {
        logMessage.with(params[i], params[i + 1]);
      }
    }
    log.info(logMessage);
  }

  private ResponseEntity<JupyterNotebookResponse> handleException(
      HttpStatus status, String message, Exception e, Object... params) {
    StringMapMessage logMessage = new StringMapMessage().with(MESSAGE_KEY, message);
    for (int i = 0; i < params.length; i += 2) {
      if (i + 1 < params.length) {
        logMessage.with(params[i].toString(), params[i + 1].toString());
      }
    }
    log.error(logMessage, e);

    var response = new JupyterNotebookErrorResponse(status.name(), message);
    return ResponseEntity.status(status).body(response);
  }

  private ResponseEntity<JupyterNotebookResponse> handleSizeLimitException(
      NotebookTooLargeException e, Object... params) {
    StringMapMessage logMessage =
        new StringMapMessage().with(MESSAGE_KEY, "Notebook size exceeds limit");
    for (int i = 0; i < params.length; i += 2) {
      if (i + 1 < params.length) {
        logMessage.with(params[i].toString(), params[i + 1].toString());
      }
    }
    log.error(logMessage, e);

    var response =
        new JupyterNotebookErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.name(), "Notebook size exceeds limit");

    // Add structured size information if available
    if (e.getMaxSizeBytes() > 0) {
      long maxSizeMB = e.getMaxSizeBytes() / (1024 * 1024);
      response.addDetail("maxSizeBytes", e.getMaxSizeBytes());
      response.addDetail("maxSizeMB", maxSizeMB);
    }
    if (e.getNotebookSizeBytes() > 0) {
      long notebookSizeMB = e.getNotebookSizeBytes() / (1024 * 1024);
      response.addDetail("notebookSizeBytes", e.getNotebookSizeBytes());
      response.addDetail("notebookSizeMB", notebookSizeMB);
    }

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
  }
}
