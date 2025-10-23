package org.jupytereverywhere.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

class ApiPrefixConfigTest {

  @Mock private PathMatchConfigurer pathMatchConfigurer;

  @InjectMocks private ApiPrefixConfig apiPrefixConfig;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testConfigurePathMatch() {
    apiPrefixConfig.configurePathMatch(pathMatchConfigurer);

    verify(pathMatchConfigurer)
        .addPathPrefix(
            eq("/api/v1"),
            argThat(predicate -> predicate.test(ApiPrefixConfigTest.RestControllerClass.class)));
  }

  @RestController
  private static class RestControllerClass {}
}
