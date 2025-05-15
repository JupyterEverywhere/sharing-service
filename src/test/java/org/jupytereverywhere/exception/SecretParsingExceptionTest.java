package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.SecretParsingException;

import static org.junit.jupiter.api.Assertions.*;

class SecretParsingExceptionTest {

  @Test
  void testSecretParsingExceptionWithMessageAndCause() {
    String errorMessage = "Error parsing secret";
    Throwable cause = new RuntimeException("Parsing error");

    SecretParsingException exception = new SecretParsingException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
