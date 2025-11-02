package org.jupytereverywhere.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SessionMismatchExceptionTest {

  @Test
  void testSessionMismatchExceptionWithMessage() {
    String errorMessage = "Session ID mismatch";

    SessionMismatchException exception = new SessionMismatchException(errorMessage);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());
  }
}
