package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import org.jupytereverywhere.exception.SecretParsingException;
import org.jupytereverywhere.exception.SecretRetrievalException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

@Log4j2
public abstract class SecretsServiceImpl implements SecretsService {

  final ObjectMapper mapper = new ObjectMapper();

  protected SecretsManagerClient secretsManager;
  protected String prefix;

  protected Map<String, String> fetchSecretValues(@NonNull String secretName) {
    final String secretNameWithPrefix = getSecretNameWithPrefix(secretName);

    try {
      GetSecretValueRequest secretValueRequest =
          GetSecretValueRequest.builder().secretId(secretNameWithPrefix).build();
      GetSecretValueResponse secretValueResponse =
          secretsManager.getSecretValue(secretValueRequest);

      String valueResult = secretValueResponse.secretString();
      return mapper.readValue(valueResult, new TypeReference<Map<String, String>>() {});
    } catch (SecretsManagerException e) {
      log.error("Error retrieving secret {}: {}", secretNameWithPrefix, e.getMessage());
      throw new SecretRetrievalException("Error retrieving secret: " + secretNameWithPrefix, e);
    } catch (JsonProcessingException e) {
      log.error(
          "Error parsing secret value result for {}: {}", secretNameWithPrefix, e.getMessage());
      throw new SecretParsingException(
          "Error parsing secret value for: " + secretNameWithPrefix, e);
    }
  }

  private String getSecretNameWithPrefix(String secretName) {
    if (secretName.startsWith(prefix)) {
      return secretName;
    }
    return prefix + secretName;
  }
}
