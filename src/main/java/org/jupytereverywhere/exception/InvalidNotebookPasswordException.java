package org.jupytereverywhere.exception;

public class InvalidNotebookPasswordException extends RuntimeException {
  public InvalidNotebookPasswordException(String message) {
    super(message);
  }
}
