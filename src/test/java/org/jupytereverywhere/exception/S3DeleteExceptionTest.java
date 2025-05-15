package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.S3DeleteException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class S3DeleteExceptionTest {

  @Test
  void testS3DeleteExceptionCreation() {
    String errorMessage = "Error deleting notebook from S3";
    Throwable cause = new RuntimeException("Underlying cause");

    S3DeleteException exception = new S3DeleteException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
