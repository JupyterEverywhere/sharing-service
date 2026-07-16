package org.jupytereverywhere.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecretComparisonUtilsTest {

  @Test
  void constantTimeEqualsAcceptsEqualValues() {
    assertTrue(SecretComparisonUtils.constantTimeEquals("matching-value", "matching-value"));
  }

  @Test
  void constantTimeEqualsRejectsDifferentValuesAndNulls() {
    assertFalse(SecretComparisonUtils.constantTimeEquals("matching-value", "different-value"));
    assertFalse(SecretComparisonUtils.constantTimeEquals(null, "matching-value"));
    assertFalse(SecretComparisonUtils.constantTimeEquals("matching-value", null));
  }
}
