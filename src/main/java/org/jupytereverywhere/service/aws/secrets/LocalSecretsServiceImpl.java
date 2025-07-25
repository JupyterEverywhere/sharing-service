package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;

@Service("secretsService")
@Profile({ "local", "test" })
public class LocalSecretsServiceImpl extends SecretsServiceImpl {

  
  public LocalSecretsServiceImpl(@Value("${aws.secrets-manager.service-endpoint}") String serviceEndpoint) {
    secretsManager = AWSSecretsManagerClientBuilder.standard()
        .withCredentials(new DefaultAWSCredentialsProviderChain())
        .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, Regions.US_WEST_1.getName()))
        .build();
    prefix = "local-";
  }

  @Override
  public Map<String, String> getSecretValues(String secretName) {
    return fetchSecretValues(secretName);
  }
}
