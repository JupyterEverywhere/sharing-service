package org.coursekata.exception;

public class S3DeleteException extends RuntimeException {
  public S3DeleteException(String message, Throwable cause) {
    super(message, cause);
  }
}
