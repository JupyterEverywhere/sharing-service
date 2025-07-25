package org.jupytereverywhere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {

  @Override
  public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
    configurer.addPathPrefix(ApiConstants.API_BASE_URL, c -> c.isAnnotationPresent(RestController.class));
  }
}

