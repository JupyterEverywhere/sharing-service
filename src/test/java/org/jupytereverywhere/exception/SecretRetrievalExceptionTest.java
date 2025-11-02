package org.jupytereverywhere.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SecretRetrievalExceptionTest {

  @Test
  void testSecretRetrievalExceptionWithMessageAndCause() {
    String errorMessage = "Error retrieving secret";
    Throwable cause = new RuntimeException("Underlying cause");

    SecretRetrievalException exception = new SecretRetrievalException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
