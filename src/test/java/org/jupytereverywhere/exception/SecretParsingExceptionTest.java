package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
