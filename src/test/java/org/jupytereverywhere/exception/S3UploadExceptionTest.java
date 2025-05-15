package org.jupytereverywhere.exception;

import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.S3UploadException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class S3UploadExceptionTest {

  @Test
  void testS3UploadExceptionCreation() {
    String errorMessage = "Error uploading notebook to S3";
    Throwable cause = new RuntimeException("Underlying cause");

    S3UploadException exception = new S3UploadException(errorMessage, cause);

    assertNotNull(exception);

    assertEquals(errorMessage, exception.getMessage());

    assertEquals(cause, exception.getCause());
  }
}
