package org.jupytereverywhere.exception;

public class NotebookNotFoundException extends RuntimeException {
  public NotebookNotFoundException(String message) {
    super(message);
  }

  public NotebookNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotebookNotFoundException() {}
}
