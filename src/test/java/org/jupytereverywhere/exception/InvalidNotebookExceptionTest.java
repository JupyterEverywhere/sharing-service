package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.InvalidNotebookException;

import static org.junit.jupiter.api.Assertions.*;

class InvalidNotebookExceptionTest {

  @Test
  void testInvalidNotebookExceptionMessage() {
    String expectedMessage = "Invalid notebook format";
    InvalidNotebookException exception = new InvalidNotebookException(expectedMessage);

    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void testInvalidNotebookExceptionThrowing() {
    String expectedMessage = "This is a test message";

    Exception exception = assertThrows(InvalidNotebookException.class, () -> {
      throw new InvalidNotebookException(expectedMessage);
    });

    assertEquals(expectedMessage, exception.getMessage());
  }
}
