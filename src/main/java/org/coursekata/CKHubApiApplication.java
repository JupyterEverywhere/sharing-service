package org.coursekata;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"org.coursekata"})
@EntityScan(basePackages = "org.coursekata.model")
@EnableJpaRepositories(basePackages = "org.coursekata.repository")
@OpenAPIDefinition
public class CKHubApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(CKHubApiApplication.class, args);
  }
}
