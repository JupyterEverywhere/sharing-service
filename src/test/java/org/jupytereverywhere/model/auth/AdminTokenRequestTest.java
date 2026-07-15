package org.jupytereverywhere.model.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AdminTokenRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void validationAcceptsSupportedTokenName() {
    assertTrue(validator.validate(new AdminTokenRequest("value", "ops-team_1.test")).isEmpty());
  }

  @Test
  void validationRejectsUnsupportedOrOversizedTokenName() {
    Set<ConstraintViolation<AdminTokenRequest>> unsupported =
        validator.validate(new AdminTokenRequest("value", "ops team"));
    Set<ConstraintViolation<AdminTokenRequest>> oversized =
        validator.validate(new AdminTokenRequest("value", "x".repeat(101)));

    assertFalse(unsupported.isEmpty());
    assertFalse(oversized.isEmpty());
  }

  @Test
  void toStringExcludesSecret() {
    String sensitiveValue = "admin-value-sentinel";
    AdminTokenRequest request = new AdminTokenRequest(sensitiveValue, "ops-team");

    assertFalse(request.toString().contains(sensitiveValue));
  }
}
