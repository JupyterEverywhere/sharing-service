package org.jupytereverywhere.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequest {
  @ToString.Exclude
  @NotBlank(message = "Token cannot be blank")
  @Size(max = 8192, message = "Token is too long")
  private String token;
}
