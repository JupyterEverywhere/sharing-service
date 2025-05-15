package org.jupytereverywhere.exception;

public class NotebookStorageException extends RuntimeException {
  public NotebookStorageException(String message) {
    super(message);
  }

  public NotebookStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
