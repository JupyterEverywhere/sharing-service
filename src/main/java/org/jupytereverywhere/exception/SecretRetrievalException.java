package org.jupytereverywhere.exception;

public class SecretRetrievalException extends RuntimeException {
  public SecretRetrievalException(String message, Throwable cause) {
    super(message, cause);
  }
}
