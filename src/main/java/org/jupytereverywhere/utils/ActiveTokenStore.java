package org.jupytereverywhere.utils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import org.jupytereverywhere.model.TokenStoreImpl;

@Service
public class ActiveTokenStore extends TokenStoreImpl {

  private final ConcurrentHashMap<UUID, String> activeTokens = new ConcurrentHashMap<>();

  @Override
  public void storeToken(UUID sessionId, String token) {
    activeTokens.put(sessionId, token);
  }

  @Override
  public void removeToken(UUID sessionId) {
    activeTokens.remove(sessionId);
  }

  @Override
  public String getToken(UUID sessionId) {
    return activeTokens.get(sessionId);
  }
}
