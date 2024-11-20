package org.coursekata.model;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TokenStoreImpl implements TokenStore {

  private final ConcurrentHashMap<UUID, String> tokenStore = new ConcurrentHashMap<>();

  @Override
  public void storeToken(UUID sessionId, String token) {
    tokenStore.put(sessionId, token);
  }

  @Override
  public String getToken(UUID sessionId) {
    return tokenStore.get(sessionId);
  }

  @Override
  public void removeToken(UUID sessionId) {
    tokenStore.remove(sessionId);
  }
}

