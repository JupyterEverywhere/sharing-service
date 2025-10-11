package org.jupytereverywhere.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  public static final String INVALID_NOTEBOOK_MESSAGE = "Invalid notebook: ";

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(INVALID_NOTEBOOK_MESSAGE + ex.getMessage());
  }

  @ExceptionHandler(InvalidNotebookException.class)
  public ResponseEntity<String> handleInvalidNotebookException(InvalidNotebookException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(INVALID_NOTEBOOK_MESSAGE + ex.getMessage());
  }

  @ExceptionHandler(NotebookTooLargeException.class)
  public ResponseEntity<String> handleNotebookTooLargeException(NotebookTooLargeException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ex.getMessage());
  }
}
