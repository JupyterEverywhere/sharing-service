package org.coursekata.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenRefreshExceptionTest {

  @Test
  void testTokenRefreshExceptionWithMessage() {
    String errorMessage = "Token refresh failed";

    TokenRefreshException exception = new TokenRefreshException(errorMessage);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());
  }

  @Test
  void testTokenRefreshExceptionWithMessageAndCause() {
    String errorMessage = "Token refresh failed";
    Throwable cause = new RuntimeException("Underlying cause");

    TokenRefreshException exception = new TokenRefreshException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
