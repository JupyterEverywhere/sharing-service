package org.jupytereverywhere.model.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@RequestScope
public class AuthenticationRequest {

  @NotBlank(message = "Notebook ID cannot be blank")
  @Size(max = 36, message = "Notebook ID is too long")
  private String notebookId;

  @ToString.Exclude
  @NotBlank(message = "Password cannot be blank")
  @Size(max = 1024, message = "Password is too long")
  private String password;
}
