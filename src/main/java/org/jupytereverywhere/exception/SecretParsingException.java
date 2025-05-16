package org.jupytereverywhere.exception;

public class SecretParsingException extends RuntimeException {
  public SecretParsingException(String message, Throwable cause) {
    super(message, cause);
  }
}
