package org.jupytereverywhere.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class S3DownloadExceptionTest {

  @Test
  void testS3DownloadExceptionCreation() {
    String errorMessage = "Error downloading notebook from S3";
    Throwable cause = new RuntimeException("Underlying cause");

    S3DownloadException exception = new S3DownloadException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
