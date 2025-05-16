package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.NotebookSerializationException;

import static org.junit.jupiter.api.Assertions.*;

class NotebookSerializationExceptionTest {

  @Test
  void testConstructorWithMessageAndCause() {
    String errorMessage = "Failed to serialize notebook";
    Throwable cause = new NullPointerException("Null value encountered");

    NotebookSerializationException exception = new NotebookSerializationException(errorMessage, cause);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }

  @Test
  void testConstructorWithNullMessageAndCause() {
    NotebookSerializationException exception = new NotebookSerializationException(null, null);

    assertNull(exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithMessageOnly() {
    String errorMessage = "Failed to serialize notebook";

    NotebookSerializationException exception = new NotebookSerializationException(errorMessage, null);

    assertEquals(errorMessage, exception.getMessage());

    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithCauseOnly() {
    Throwable cause = new IllegalArgumentException("Invalid notebook format");

    NotebookSerializationException exception = new NotebookSerializationException(null, cause);

    assertNull(exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
