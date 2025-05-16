package org.jupytereverywhere.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.jupytereverywhere.filter.JwtRequestFilter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@Disabled
@AutoConfigureMockMvc
class SecurityConfigTest {

  @MockBean
  private JwtRequestFilter jwtRequestFilter;

  @Autowired
  private SecurityConfig securityConfig;

  @Autowired
  private HttpSecurity httpSecurity;

  @Test
  void testSecurityFilterChain() throws Exception {
    doNothing().when(jwtRequestFilter).doFilter(any(), any(), any());

    SecurityFilterChain result = securityConfig.securityFilterChain(httpSecurity);
    assertNotNull(result, "SecurityFilterChain should not be null");
  }
}
