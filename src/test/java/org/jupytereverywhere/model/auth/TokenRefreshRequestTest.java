package org.jupytereverywhere.model.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRefreshRequestTest {

  private TokenRefreshRequest tokenRefreshRequest;

  @BeforeEach
  public void setUp() {
    tokenRefreshRequest = new TokenRefreshRequest();
  }

  @Test
  void testNoArgsConstructor() {
    TokenRefreshRequest emptyRequest = new TokenRefreshRequest();
    assertNull(emptyRequest.getToken(), "Token should be null when using the no-args constructor");
  }

  @Test
  void testAllArgsConstructor() {
    TokenRefreshRequest request = new TokenRefreshRequest("sampleToken");
    assertEquals("sampleToken", request.getToken(), "Token should match the value set via the all-args constructor");
  }

  @Test
  void testSetAndGetToken() {
    tokenRefreshRequest.setToken("sampleToken");
    assertEquals("sampleToken", tokenRefreshRequest.getToken(), "Token should be set and retrieved correctly");
  }

  @Test
  void testToString() {
    tokenRefreshRequest.setToken("sampleToken");
    String expectedString = "TokenRefreshRequest(token=sampleToken)";
    assertEquals(expectedString, tokenRefreshRequest.toString(), "toString output should match the expected format");
  }

  @Test
  void testEquals_SameObject() {
    assertEquals(tokenRefreshRequest, tokenRefreshRequest, "An object should be equal to itself");
  }

  @Test
  void testEquals_DifferentObjectsSameValues() {
    TokenRefreshRequest request1 = new TokenRefreshRequest("sampleToken");
    TokenRefreshRequest request2 = new TokenRefreshRequest("sampleToken");
    assertEquals(request1, request2, "Objects with the same values should be equal");
  }

  @Test
  void testEquals_NullObject() {
    TokenRefreshRequest request1 = new TokenRefreshRequest("sampleToken");
    assertNotEquals(null, request1, "An object should not be equal to null");
  }

  @Test
  void testEquals_DifferentType() {
    TokenRefreshRequest request1 = new TokenRefreshRequest("sampleToken");
    assertNotEquals("Some String", request1, "An object should not be equal to an object of a different type");
  }

  @Test
  void testEquals_DifferentValues() {
    TokenRefreshRequest request1 = new TokenRefreshRequest("sampleToken");
    TokenRefreshRequest request2 = new TokenRefreshRequest("differentToken");
    assertNotEquals(request1, request2, "Objects with different values should not be equal");
  }

  @Test
  void testHashCode_SameValues() {
    TokenRefreshRequest request1 = new TokenRefreshRequest("sampleToken");
    TokenRefreshRequest request2 = new TokenRefreshRequest("sampleToken");
    assertEquals(request1.hashCode(), request2.hashCode(), "HashCode should be the same for objects with the same values");
  }

  @Test
  void testHashCode_DifferentValues() {
    TokenRefreshRequest request1 = new TokenRefreshRequest("sampleToken");
    TokenRefreshRequest request2 = new TokenRefreshRequest("differentToken");
    assertNotEquals(request1.hashCode(), request2.hashCode(), "HashCode should be different for objects with different values");
  }

  @Test
  void testCanEqual() {
    TokenRefreshRequest request1 = new TokenRefreshRequest();
    assertTrue(request1.canEqual(new TokenRefreshRequest()), "canEqual should return true for objects of the same type");
  }

  @Test
  void testCanEqual_DifferentType() {
    TokenRefreshRequest request = new TokenRefreshRequest();
    assertFalse(request.canEqual("Some String"), "canEqual should return false for objects of different types");
  }
}
