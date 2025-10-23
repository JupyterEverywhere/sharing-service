package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class ConfigurableSecretsServiceImpl extends SecretsServiceImpl {
  public ConfigurableSecretsServiceImpl(SecretsManagerClient secretsManager, String prefix) {
    this.secretsManager = secretsManager;
    this.prefix = prefix;
  }

  @Override
  public Map<String, String> getSecretValues(String secretName) {
    return fetchSecretValues(secretName);
  }
}
