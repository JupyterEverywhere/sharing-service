package org.coursekata.model;

import java.util.UUID;

public interface TokenStore {
  void storeToken(UUID sessionId, String token);
  void removeToken(UUID sessionId);
  String getToken(UUID sessionId);
}
