package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotebookStorageExceptionTest {

  @Test
  void testNotebookStorageExceptionWithMessage() {
    String errorMessage = "Error storing notebook";

    NotebookStorageException exception = new NotebookStorageException(errorMessage);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  void testNotebookStorageExceptionWithMessageAndCause() {
    String errorMessage = "Error storing notebook";
    Throwable cause = new RuntimeException("Storage failure");

    NotebookStorageException exception = new NotebookStorageException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
