package org.jupytereverywhere.model;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenStoreImplTest {

  private TokenStoreImpl tokenStore;

  @BeforeEach
  void setUp() {
    tokenStore = new TokenStoreImpl();
  }

  @Test
  void testStoreToken() {
    UUID sessionId = UUID.randomUUID();
    String token = "sample-token";

    tokenStore.storeToken(sessionId, token);

    assertEquals(token, tokenStore.getToken(sessionId), "Token should be stored and retrievable");
  }

  @Test
  void testGetToken_WhenTokenExists() {
    UUID sessionId = UUID.randomUUID();
    String token = "sample-token";

    tokenStore.storeToken(sessionId, token);

    String retrievedToken = tokenStore.getToken(sessionId);
    assertNotNull(retrievedToken, "Token should not be null");
    assertEquals(token, retrievedToken, "Retrieved token should match the stored token");
  }

  @Test
  void testGetToken_WhenTokenDoesNotExist() {
    UUID sessionId = UUID.randomUUID();

    String retrievedToken = tokenStore.getToken(sessionId);
    assertNull(retrievedToken, "Token should be null when not found");
  }

  @Test
  void testRemoveToken_WhenTokenExists() {
    UUID sessionId = UUID.randomUUID();
    String token = "sample-token";

    tokenStore.storeToken(sessionId, token);
    tokenStore.removeToken(sessionId);

    assertNull(tokenStore.getToken(sessionId), "Token should be removed and not retrievable");
  }

  @Test
  void testRemoveToken_WhenTokenDoesNotExist() {
    UUID sessionId = UUID.randomUUID();

    assertDoesNotThrow(() -> tokenStore.removeToken(sessionId), "Removing a non-existent token should not throw an exception");
  }
}
