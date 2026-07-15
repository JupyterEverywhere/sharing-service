package org.jupytereverywhere.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

class AuthExceptionHandlerTest {

  @Test
  void malformedJsonResponseDoesNotExposeParserDetails() {
    String sensitiveDetail = "submitted-credential-sentinel";
    HttpMessageNotReadableException exception =
        new HttpMessageNotReadableException(sensitiveDetail, mock(HttpInputMessage.class));

    ResponseEntity<String> response =
        new AuthExceptionHandler().handleHttpMessageNotReadableException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(AuthExceptionHandler.MALFORMED_JSON_MESSAGE, response.getBody());
    assertFalse(response.getBody().contains(sensitiveDetail));
  }

  @Test
  void validationResponseDoesNotExposeRejectedValue() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);

    ResponseEntity<String> response =
        new AuthExceptionHandler().handleMethodArgumentNotValidException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(AuthExceptionHandler.INVALID_REQUEST_MESSAGE, response.getBody());
  }
}
