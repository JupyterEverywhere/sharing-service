package org.jupytereverywhere.exception;

public class NotebookSerializationException extends RuntimeException {
  public NotebookSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
