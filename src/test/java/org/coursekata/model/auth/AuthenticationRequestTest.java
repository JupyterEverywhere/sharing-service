package org.coursekata.model.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationRequestTest {

  private AuthenticationRequest authenticationRequest;

  @BeforeEach
  public void setUp() {
    authenticationRequest = new AuthenticationRequest();
  }

  @Test
  void testNoArgsConstructor() {
    AuthenticationRequest emptyRequest = new AuthenticationRequest();
    assertNull(emptyRequest.getUsername());
    assertNull(emptyRequest.getPassword());
  }

  @Test
  void testAllArgsConstructor() {
    AuthenticationRequest request = new AuthenticationRequest("testUser", "testPassword");
    assertEquals("testUser", request.getUsername());
    assertEquals("testPassword", request.getPassword());
  }

  @Test
  void testSetAndGetUsername() {
    authenticationRequest.setUsername("testUser");
    assertEquals("testUser", authenticationRequest.getUsername());
  }

  @Test
  void testSetAndGetPassword() {
    authenticationRequest.setPassword("testPassword");
    assertEquals("testPassword", authenticationRequest.getPassword());
  }

  @Test
  void testToString() {
    authenticationRequest.setUsername("testUser");
    authenticationRequest.setPassword("testPassword");
    String expectedString = "AuthenticationRequest(username=testUser, password=testPassword)";
    assertEquals(expectedString, authenticationRequest.toString());
  }

  @Test
  void testEquals_SameObject() {
    assertEquals(authenticationRequest, authenticationRequest, "An object should be equal to itself");
  }

  @Test
  void testEquals_DifferentObjectsSameValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("testUser", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("testUser", "testPassword");
    assertEquals(request1, request2, "Objects with the same values should be equal");
  }

  @Test
  void testEquals_NullObject() {
    AuthenticationRequest request1 = new AuthenticationRequest("testUser", "testPassword");
    assertNotEquals(null, request1, "An object should not be equal to null");
  }

  @Test
  void testEquals_DifferentType() {
    AuthenticationRequest request1 = new AuthenticationRequest("testUser", "testPassword");
    assertNotEquals("Some String", request1, "An object should not be equal to an object of a different type");
  }

  @Test
  void testEquals_DifferentValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("testUser", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("otherUser", "otherPassword");
    assertNotEquals(request1, request2, "Objects with different values should not be equal");
  }

  @Test
  void testHashCode_SameValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("testUser", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("testUser", "testPassword");
    assertEquals(request1.hashCode(), request2.hashCode(), "HashCode should be the same for objects with the same values");
  }

  @Test
  void testHashCode_DifferentValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("testUser", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("otherUser", "otherPassword");
    assertNotEquals(request1.hashCode(), request2.hashCode(), "HashCode should be different for objects with different values");
  }

  @Test
  void testCanEqual() {
    AuthenticationRequest request1 = new AuthenticationRequest();
    assertTrue(request1.canEqual(new AuthenticationRequest()), "canEqual should return true for objects of the same type");
  }

  @Test
  void testCanEqual_DifferentType() {
    AuthenticationRequest request = new AuthenticationRequest();
    assertFalse(request.canEqual("Some String"), "canEqual should return false for objects of different types");
  }
}
