package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

public interface SecretsService {
  Map<String, String> getSecretValues(String secretName);
}
