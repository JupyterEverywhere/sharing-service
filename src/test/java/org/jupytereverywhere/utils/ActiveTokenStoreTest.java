package org.jupytereverywhere.utils;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.utils.ActiveTokenStore;

class ActiveTokenStoreTest {

  private ActiveTokenStore activeTokenStore;

  @BeforeEach
  void setUp() {
    activeTokenStore = new ActiveTokenStore();
  }

  @Test
  void testStoreToken() {
    UUID sessionId = UUID.randomUUID();
    String token = "some-jwt-token";

    activeTokenStore.storeToken(sessionId, token);
    assertEquals(token, activeTokenStore.getToken(sessionId));
  }

  @Test
  void testGetToken_TokenExists() {
    UUID sessionId = UUID.randomUUID();
    String token = "some-jwt-token";
    activeTokenStore.storeToken(sessionId, token);

    String retrievedToken = activeTokenStore.getToken(sessionId);
    assertNotNull(retrievedToken);
    assertEquals(token, retrievedToken);
  }

  @Test
  void testGetToken_TokenDoesNotExist() {
    UUID sessionId = UUID.randomUUID();
    String token = activeTokenStore.getToken(sessionId);

    assertNull(token);
  }

  @Test
  void testRemoveToken_TokenExists() {
    UUID sessionId = UUID.randomUUID();
    String token = "some-jwt-token";
    activeTokenStore.storeToken(sessionId, token);

    activeTokenStore.removeToken(sessionId);
    assertNull(activeTokenStore.getToken(sessionId));
  }

  @Test
  void testRemoveToken_TokenDoesNotExist() {
    UUID sessionId = UUID.randomUUID();
    activeTokenStore.removeToken(sessionId);

    assertNull(activeTokenStore.getToken(sessionId));
  }
}
