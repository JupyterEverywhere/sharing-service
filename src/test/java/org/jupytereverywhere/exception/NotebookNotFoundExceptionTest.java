package org.jupytereverywhere.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NotebookNotFoundExceptionTest {

  @Test
  void testNotebookNotFoundExceptionWithMessage() {
    String errorMessage = "Notebook not found";

    NotebookNotFoundException exception = new NotebookNotFoundException(errorMessage);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  void testNotebookNotFoundExceptionWithMessageAndCause() {
    String errorMessage = "Notebook not found";
    Throwable cause = new RuntimeException("Database failure");

    NotebookNotFoundException exception = new NotebookNotFoundException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
