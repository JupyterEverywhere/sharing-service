package org.jupytereverywhere.model.auth;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class AuthenticationResponse {
  @ToString.Exclude private String token;
}
