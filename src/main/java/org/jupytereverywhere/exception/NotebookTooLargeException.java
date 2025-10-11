package org.jupytereverywhere.exception;

public class NotebookTooLargeException extends RuntimeException {
  public NotebookTooLargeException(String message) {
    super(message);
  }
}
