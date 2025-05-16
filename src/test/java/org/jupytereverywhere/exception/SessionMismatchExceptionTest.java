package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionMismatchExceptionTest {

  @Test
  void testSessionMismatchExceptionWithMessage() {
    String errorMessage = "Session ID mismatch";

    SessionMismatchException exception = new SessionMismatchException(errorMessage);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());
  }
}
