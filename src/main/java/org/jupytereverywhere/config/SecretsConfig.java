package org.jupytereverywhere.config;

import java.net.URI;

import org.jupytereverywhere.service.aws.secrets.ConfigurableSecretsServiceImpl;
import org.jupytereverywhere.service.aws.secrets.SecretsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

@Configuration
public class SecretsConfig {

  @Value("${aws.secrets-manager.service-endpoint:}")
  private String serviceEndpoint;

  @Value("${aws.secrets-manager.region:us-east-1}")
  private String region;

  @Value("${aws.secrets-manager.prefix:}")
  private String prefix;

  @Value("${aws.s3.secret-name:}")
  private String s3SecretName;

  @Bean(name = "secretsService")
  @ConditionalOnProperty(name = "aws.s3.secret-name", matchIfMissing = false)
  public SecretsService secretsService() {

    Region awsRegion;
    try {
      awsRegion = Region.of(region);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid AWS region for secrets manager: '" + region + "'", e);
    }

    SecretsManagerClientBuilder builder = SecretsManagerClient.builder().region(awsRegion);

    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      builder.endpointOverride(URI.create(serviceEndpoint));
    }

    SecretsManagerClient secretsManager = builder.build();
    return new ConfigurableSecretsServiceImpl(secretsManager, prefix);
  }
}
