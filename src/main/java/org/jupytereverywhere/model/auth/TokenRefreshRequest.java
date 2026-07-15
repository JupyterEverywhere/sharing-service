package org.jupytereverywhere.model.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequest {
  @ToString.Exclude private String token;
}
