package org.coursekata.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class InvalidTokenExceptionTest {

  @Test
  void testInvalidTokenExceptionMessage() {
    String expectedMessage = "Invalid token provided";
    InvalidTokenException exception = new InvalidTokenException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void testInvalidTokenExceptionWithResponseStatus() {
    String expectedMessage = "Unauthorized access due to invalid token";

    InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
      throw new InvalidTokenException(expectedMessage);
    });

    assertEquals(expectedMessage, exception.getMessage());

    ResponseStatus responseStatus = InvalidTokenException.class.getAnnotation(ResponseStatus.class);
    assertEquals(HttpStatus.UNAUTHORIZED, responseStatus.value());
  }
}
