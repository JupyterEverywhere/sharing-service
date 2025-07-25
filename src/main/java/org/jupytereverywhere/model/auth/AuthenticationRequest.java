package org.jupytereverywhere.model.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.validation.constraints.NotBlank;
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
  private String notebookId;

  @ToString.Exclude
  @NotBlank(message = "Password cannot be blank")
  private String password;
}
