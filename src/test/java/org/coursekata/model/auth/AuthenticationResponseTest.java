package org.coursekata.model.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationResponseTest {

  private AuthenticationResponse authenticationResponse;

  @BeforeEach
  public void setUp() {
    authenticationResponse = new AuthenticationResponse();
  }

  @Test
  void testNoArgsConstructor() {
    AuthenticationResponse emptyResponse = new AuthenticationResponse();
    assertNull(emptyResponse.getToken());
  }

  @Test
  void testAllArgsConstructor() {
    AuthenticationResponse response = new AuthenticationResponse("sampleToken");
    assertEquals("sampleToken", response.getToken());
  }

  @Test
  void testSetAndGetToken() {
    authenticationResponse.setToken("sampleToken");
    assertEquals("sampleToken", authenticationResponse.getToken());
  }

  @Test
  void testToString() {
    authenticationResponse.setToken("sampleToken");
    String expectedString = "AuthenticationResponse(token=sampleToken)";
    assertEquals(expectedString, authenticationResponse.toString());
  }

  @Test
  void testEquals_SameObject() {
    assertEquals(authenticationResponse, authenticationResponse, "An object should be equal to itself");
  }

  @Test
  void testEquals_DifferentObjectsSameValues() {
    AuthenticationResponse response1 = new AuthenticationResponse("sampleToken");
    AuthenticationResponse response2 = new AuthenticationResponse("sampleToken");
    assertEquals(response1, response2, "Objects with the same values should be equal");
  }

  @Test
  void testEquals_NullObject() {
    AuthenticationResponse response1 = new AuthenticationResponse("sampleToken");
    assertNotEquals(null, response1, "An object should not be equal to null");
  }

  @Test
  void testEquals_DifferentType() {
    AuthenticationResponse response1 = new AuthenticationResponse("sampleToken");
    assertNotEquals("Some String", response1, "An object should not be equal to an object of a different type");
  }

  @Test
  void testEquals_DifferentValues() {
    AuthenticationResponse response1 = new AuthenticationResponse("sampleToken");
    AuthenticationResponse response2 = new AuthenticationResponse("differentToken");
    assertNotEquals(response1, response2, "Objects with different values should not be equal");
  }

  @Test
  void testHashCode_SameValues() {
    AuthenticationResponse response1 = new AuthenticationResponse("sampleToken");
    AuthenticationResponse response2 = new AuthenticationResponse("sampleToken");
    assertEquals(response1.hashCode(), response2.hashCode(), "HashCode should be the same for objects with the same values");
  }

  @Test
  void testHashCode_DifferentValues() {
    AuthenticationResponse response1 = new AuthenticationResponse("sampleToken");
    AuthenticationResponse response2 = new AuthenticationResponse("differentToken");
    assertNotEquals(response1.hashCode(), response2.hashCode(), "HashCode should be different for objects with different values");
  }

  @Test
  void testCanEqual() {
    AuthenticationResponse response1 = new AuthenticationResponse();
    assertTrue(response1.canEqual(new AuthenticationResponse()), "canEqual should return true for objects of the same type");
  }

  @Test
  void testCanEqual_DifferentType() {
    AuthenticationResponse response = new AuthenticationResponse();
    assertFalse(response.canEqual("Some String"), "canEqual should return false for objects of different types");
  }
}
