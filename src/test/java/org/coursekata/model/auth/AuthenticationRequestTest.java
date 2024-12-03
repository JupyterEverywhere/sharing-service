package org.coursekata.model.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    assertNull(emptyRequest.getNotebookId(), "NotebookId should be null");
    assertNull(emptyRequest.getPassword(), "Password should be null");
  }

  @Test
  void testAllArgsConstructor() {
    AuthenticationRequest request = new AuthenticationRequest("notebook-123", "testPassword");
    assertEquals("notebook-123", request.getNotebookId());
    assertEquals("testPassword", request.getPassword());
  }

  @Test
  void testSetAndGetNotebookId() {
    authenticationRequest.setNotebookId("notebook-123");
    assertEquals("notebook-123", authenticationRequest.getNotebookId());
  }

  @Test
  void testSetAndGetPassword() {
    authenticationRequest.setPassword("testPassword");
    assertEquals("testPassword", authenticationRequest.getPassword());
  }

  @Test
  void testToString_ExcludesPassword() {
    authenticationRequest.setNotebookId("notebook-123");
    authenticationRequest.setPassword("testPassword");
    String expectedString = "AuthenticationRequest(notebookId=notebook-123)";
    assertEquals(expectedString, authenticationRequest.toString(), "Password should be excluded from toString()");
  }

  @Test
  void testEquals_SameObject() {
    assertEquals(authenticationRequest, authenticationRequest, "An object should be equal to itself");
  }

  @Test
  void testEquals_DifferentObjectsSameValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("notebook-123", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("notebook-123", "testPassword");
    assertEquals(request1, request2, "Objects with the same values should be equal");
  }

  @Test
  void testEquals_NullObject() {
    AuthenticationRequest request1 = new AuthenticationRequest("notebook-123", "testPassword");
    assertNotEquals(null, request1, "An object should not be equal to null");
  }

  @Test
  void testEquals_DifferentType() {
    AuthenticationRequest request = new AuthenticationRequest("notebook-123", "testPassword");
    assertNotEquals("Some String", request, "An object should not be equal to an object of a different type");
  }

  @Test
  void testEquals_DifferentValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("notebook-123", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("notebook-456", "otherPassword");
    assertNotEquals(request1, request2, "Objects with different values should not be equal");
  }

  @Test
  void testHashCode_SameValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("notebook-123", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("notebook-123", "testPassword");
    assertEquals(request1.hashCode(), request2.hashCode(), "HashCode should be the same for objects with the same values");
  }

  @Test
  void testHashCode_DifferentValues() {
    AuthenticationRequest request1 = new AuthenticationRequest("notebook-123", "testPassword");
    AuthenticationRequest request2 = new AuthenticationRequest("notebook-456", "otherPassword");
    assertNotEquals(request1.hashCode(), request2.hashCode(), "HashCode should be different for objects with different values");
  }

  @Test
  void testNotBlankValidation_NotebookId() {
    AuthenticationRequest request = new AuthenticationRequest("", "testPassword");
    assertTrue(request.getNotebookId().isBlank(), "NotebookId should be blank");
  }

  @Test
  void testNotBlankValidation_Password() {
    AuthenticationRequest request = new AuthenticationRequest("notebook-123", "");
    assertTrue(request.getPassword().isBlank(), "Password should be blank");
  }
}
