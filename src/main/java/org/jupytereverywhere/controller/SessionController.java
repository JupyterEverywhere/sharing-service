package org.jupytereverywhere.controller;

import java.util.UUID;

import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.dto.SessionDeleteResponse;
import org.jupytereverywhere.service.JupyterNotebookService;
import org.jupytereverywhere.service.JwtTokenService;
import org.jupytereverywhere.utils.HttpHeaderUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/sessions")
public class SessionController {

  private static final String MESSAGE_KEY = "Message";
  private static final String SESSION_ID_KEY = "SessionID";

  private final JupyterNotebookService notebookService;
  private final JwtTokenService jwtTokenService;

  public SessionController(
      JupyterNotebookService notebookService, JwtTokenService jwtTokenService) {
    this.notebookService = notebookService;
    this.jwtTokenService = jwtTokenService;
  }

  @DeleteMapping("/{sessionId}/notebooks")
  public ResponseEntity<?> deleteNotebooksBySession(
      @PathVariable UUID sessionId, HttpServletRequest request) {
    String adminTokenName = HttpHeaderUtils.extractAdminTokenName(request, jwtTokenService);
    log.info(
        new StringMapMessage()
            .with(MESSAGE_KEY, "Received admin session delete request")
            .with(SESSION_ID_KEY, sessionId.toString()));

    try {
      int deletedCount = notebookService.deleteNotebooksBySessionId(sessionId, adminTokenName);
      log.info(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Session notebooks deleted successfully")
              .with(SESSION_ID_KEY, sessionId.toString())
              .with("DeletedCount", String.valueOf(deletedCount)));
      return ResponseEntity.ok(new SessionDeleteResponse(deletedCount));
    } catch (Exception e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE_KEY, "Failed to delete session notebooks")
              .with(SESSION_ID_KEY, sessionId.toString()),
          e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
