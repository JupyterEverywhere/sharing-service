package org.jupytereverywhere.exception;

public class InvalidNotebookException extends RuntimeException {
  public InvalidNotebookException(String message) {
    super(message);
  }
}
