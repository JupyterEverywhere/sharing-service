package org.jupytereverywhere;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"org.jupytereverywhere"})
@EntityScan(basePackages = "org.jupytereverywhere.model")
@EnableJpaRepositories(basePackages = "org.jupytereverywhere.repository")
@OpenAPIDefinition
public class SharingServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(SharingServiceApplication.class, args);
  }
}
