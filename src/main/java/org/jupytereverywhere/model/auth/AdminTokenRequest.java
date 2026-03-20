package org.jupytereverywhere.model.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminTokenRequest {

  @NotBlank(message = "Secret cannot be blank")
  @ToString.Exclude
  private String secret;

  @NotBlank(message = "Token name cannot be blank")
  private String tokenName;
}
