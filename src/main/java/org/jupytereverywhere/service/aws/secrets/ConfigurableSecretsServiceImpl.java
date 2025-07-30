package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;

public class ConfigurableSecretsServiceImpl extends SecretsServiceImpl {
    public ConfigurableSecretsServiceImpl(AWSSecretsManager secretsManager, String prefix) {
        this.secretsManager = secretsManager;
        this.prefix = prefix;
    }

    @Override
    public Map<String, String> getSecretValues(String secretName) {
        return fetchSecretValues(secretName);
    }
}
