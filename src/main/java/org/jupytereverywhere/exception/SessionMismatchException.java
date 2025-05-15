package org.jupytereverywhere.exception;

public class SessionMismatchException extends RuntimeException {
  public SessionMismatchException(String message) {
    super(message);
  }
}
