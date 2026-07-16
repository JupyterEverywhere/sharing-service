package org.jupytereverywhere.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminTokenRequest {

  @NotBlank(message = "Secret cannot be blank")
  @Size(max = 8192, message = "Secret is too long")
  @ToString.Exclude
  private String secret;

  @NotBlank(message = "Token name cannot be blank")
  @Size(max = 100, message = "Token name is too long")
  @Pattern(
      regexp = "[A-Za-z0-9][A-Za-z0-9._-]*",
      message = "Token name contains unsupported characters")
  private String tokenName;
}
