package org.jupytereverywhere.exception;

public class UnauthorizedNotebookAccessException extends RuntimeException {
  public UnauthorizedNotebookAccessException(String message) {
    super(message);
  }
}
