package org.jupytereverywhere.model;

import java.util.UUID;

public interface TokenStore {
  void storeToken(UUID sessionId, String token);

  void removeToken(UUID sessionId);

  String getToken(UUID sessionId);
}
