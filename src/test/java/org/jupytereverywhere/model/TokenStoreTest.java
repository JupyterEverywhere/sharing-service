package org.jupytereverywhere.model;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenStoreTest {

  private TokenStore tokenStore;

  @BeforeEach
  void setUp() {
    tokenStore = new TokenStoreImpl();
  }

  @Test
  void testStoreToken() {
    UUID sessionId = UUID.randomUUID();
    String token = "some-jwt-token";

    tokenStore.storeToken(sessionId, token);
    assertEquals(token, tokenStore.getToken(sessionId));
  }

  @Test
  void testGetToken_TokenExists() {
    UUID sessionId = UUID.randomUUID();
    String token = "some-jwt-token";
    tokenStore.storeToken(sessionId, token);

    String retrievedToken = tokenStore.getToken(sessionId);
    assertNotNull(retrievedToken);
    assertEquals(token, retrievedToken);
  }

  @Test
  void testGetToken_TokenDoesNotExist() {
    UUID sessionId = UUID.randomUUID();
    String token = tokenStore.getToken(sessionId);

    assertNull(token);
  }

  @Test
  void testRemoveToken_TokenExists() {
    UUID sessionId = UUID.randomUUID();
    String token = "some-jwt-token";
    tokenStore.storeToken(sessionId, token);

    tokenStore.removeToken(sessionId);
    assertNull(tokenStore.getToken(sessionId));
  }

  @Test
  void testRemoveToken_TokenDoesNotExist() {
    UUID sessionId = UUID.randomUUID();
    tokenStore.removeToken(sessionId);

    assertNull(tokenStore.getToken(sessionId));
  }
}
