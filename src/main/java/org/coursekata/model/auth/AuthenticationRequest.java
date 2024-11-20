package org.coursekata.model.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@RequestScope
public class AuthenticationRequest {
  private String username;
  private String password;
}
