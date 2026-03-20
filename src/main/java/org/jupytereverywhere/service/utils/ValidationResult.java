package org.jupytereverywhere.service.utils;

public record ValidationResult(boolean valid, String errorMessage) {
  public static ValidationResult success() {
    return new ValidationResult(true, null);
  }

  public static ValidationResult failure(String message) {
    return new ValidationResult(false, message);
  }
}
