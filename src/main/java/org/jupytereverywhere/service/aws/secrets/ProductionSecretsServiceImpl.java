package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;

@Service("secretsService")
@Profile("production")
public class ProductionSecretsServiceImpl extends SecretsServiceImpl {

  public ProductionSecretsServiceImpl() {
    secretsManager = AWSSecretsManagerClientBuilder.standard()
        .withCredentials(new DefaultAWSCredentialsProviderChain())
        .withRegion(Regions.US_WEST_1)
        .build();
    prefix = "production-";
  }

  @Override
  public Map<String, String> getSecretValues(String secretName) {
    return fetchSecretValues(secretName);
  }
}
