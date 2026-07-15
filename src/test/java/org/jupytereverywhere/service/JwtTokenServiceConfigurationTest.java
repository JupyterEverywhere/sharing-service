package org.jupytereverywhere.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(OutputCaptureExtension.class)
class JwtTokenServiceConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
          .withBean(PasswordEncoder.class, BCryptPasswordEncoder::new)
          .withBean(JwtTokenService.class);

  @Test
  void missingSigningKeyFailsContextStartup() {
    contextRunner
        .withPropertyValues("security.jwt.token.expiration-minutes=60")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void invalidSigningKeyFailsWithoutLoggingItsValue(CapturedOutput output) {
    String invalidValue = "invalid-value-sentinel";

    contextRunner
        .withPropertyValues(
            "security.jwt.token.secret-key=" + invalidValue,
            "security.jwt.token.expiration-minutes=60")
        .run(context -> assertThat(context).hasFailed());

    assertFalse(output.getAll().contains(invalidValue));
  }

  @Test
  void validSigningConfigurationStartsContext() {
    contextRunner
        .withPropertyValues(
            "security.jwt.token.secret-key=12345678901234567890123456789012",
            "security.jwt.token.expiration-minutes=60")
        .run(context -> assertThat(context).hasSingleBean(JwtTokenService.class));
  }

  @Test
  void applicationPropertiesHasNoSigningKeyFallback() throws IOException {
    Properties properties = new Properties();
    try (InputStream input =
        getClass().getClassLoader().getResourceAsStream("application.properties")) {
      assertThat(input).isNotNull();
      properties.load(input);
    }

    assertEquals("${JWT_SECRET_KEY}", properties.getProperty("security.jwt.token.secret-key"));
  }
}
