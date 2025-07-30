package org.jupytereverywhere.config;

import org.jupytereverywhere.service.aws.secrets.SecretsService;
import org.jupytereverywhere.service.aws.secrets.ConfigurableSecretsServiceImpl;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.regions.Regions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

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

        Regions awsRegion;
        try {
            awsRegion = Regions.fromName(region);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid AWS region for secrets manager: '" + region + "'", e);
        }

        AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain());
        if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
            builder.withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, awsRegion.getName()));
        } else {
            builder.withRegion(awsRegion);
        }

        AWSSecretsManager secretsManager = builder.build();
        return new ConfigurableSecretsServiceImpl(secretsManager, prefix);
    }
}
