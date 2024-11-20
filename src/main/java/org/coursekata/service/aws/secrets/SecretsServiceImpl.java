package org.coursekata.service.aws.secrets;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.coursekata.exception.SecretParsingException;
import org.coursekata.exception.SecretRetrievalException;

@Log4j2
public abstract class SecretsServiceImpl implements SecretsService {

  final ObjectMapper mapper = new ObjectMapper();

  protected AWSSecretsManager secretsManager;
  protected String prefix;

  protected Map<String, String> fetchSecretValues(@NonNull String secretName) {
    final String secretNameWithPrefix = getSecretNameWithPrefix(secretName);

    try {
      GetSecretValueRequest secretValueRequest = new GetSecretValueRequest().withSecretId(secretNameWithPrefix);
      GetSecretValueResult secretValueResult = secretsManager.getSecretValue(secretValueRequest);

      String valueResult = secretValueResult.getSecretString();
      return mapper.readValue(valueResult, new TypeReference<Map<String, String>>() {});
    } catch (AWSSecretsManagerException e) {
      log.error("Error retrieving secret {}: {}", secretNameWithPrefix, e.getMessage());
      throw new SecretRetrievalException("Error retrieving secret: " + secretNameWithPrefix, e);
    } catch (JsonProcessingException e) {
      log.error("Error parsing secret value result for {}: {}", secretNameWithPrefix, e.getMessage());
      throw new SecretParsingException("Error parsing secret value for: " + secretNameWithPrefix, e);
    }
  }

  private String getSecretNameWithPrefix(String secretName) {
    if (secretName.startsWith(prefix)) {
      return secretName;
    }
    return prefix + secretName;
  }
}
