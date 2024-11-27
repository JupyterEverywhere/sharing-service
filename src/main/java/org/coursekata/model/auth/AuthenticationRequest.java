package org.coursekata.model.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.validation.constraints.NotBlank;

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
