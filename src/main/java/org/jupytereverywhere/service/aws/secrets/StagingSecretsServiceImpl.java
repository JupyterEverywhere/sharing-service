package org.jupytereverywhere.service.aws.secrets;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service("secretsService")
@Profile("staging")
public class StagingSecretsServiceImpl extends SecretsServiceImpl {

  public StagingSecretsServiceImpl() {
    secretsManager = AWSSecretsManagerClientBuilder.standard()
        .withCredentials(new DefaultAWSCredentialsProviderChain())
        .withRegion(Regions.US_WEST_1)
        .build();
    prefix = "staging-";
  }

  @Override
  public Map<String, String> getSecretValues(String secretName) {
    return fetchSecretValues(secretName);
  }
}
