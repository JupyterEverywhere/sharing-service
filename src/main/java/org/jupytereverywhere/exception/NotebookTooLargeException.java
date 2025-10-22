package org.jupytereverywhere.exception;

import lombok.Getter;

@Getter
public class NotebookTooLargeException extends RuntimeException {
  private final long notebookSizeBytes;
  private final long maxSizeBytes;

  public NotebookTooLargeException(String message) {
    super(message);
    this.notebookSizeBytes = 0;
    this.maxSizeBytes = 0;
  }

  public NotebookTooLargeException(String message, long notebookSizeBytes, long maxSizeBytes) {
    super(message);
    this.notebookSizeBytes = notebookSizeBytes;
    this.maxSizeBytes = maxSizeBytes;
  }
}
