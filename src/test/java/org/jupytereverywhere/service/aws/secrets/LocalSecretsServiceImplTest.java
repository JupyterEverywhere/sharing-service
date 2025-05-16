package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalSecretsServiceImplTest {

  @Mock
  private AWSSecretsManager secretsManager;

  private LocalSecretsServiceImpl localSecretsService;

  private String serviceEndpoint = "https://mock-secrets-endpoint";

  @BeforeEach
  void setUp() {
    localSecretsService = new LocalSecretsServiceImpl(serviceEndpoint);
    localSecretsService.secretsManager = secretsManager;
  }

  @Test
  void testGetSecretValues_Success() {
    String secretName = "test-secret";
    String secretJson = "{\"key1\":\"value1\", \"key2\":\"value2\"}";

    GetSecretValueResult result = new GetSecretValueResult().withSecretString(secretJson);
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(result);

    Map<String, String> secretValues = localSecretsService.getSecretValues(secretName);

    assertEquals(2, secretValues.size());
    assertEquals("value1", secretValues.get("key1"));
    assertEquals("value2", secretValues.get("key2"));
  }

  @Test
  void testGetSecretValues_SecretRetrievalError() {
    String secretName = "test-secret";

    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(new RuntimeException("Error retrieving secret"));

    assertThrows(RuntimeException.class, () -> {
      localSecretsService.getSecretValues(secretName);
    });
  }

  @Test
  void testConstructor_ServiceEndpoint() {
    assertEquals("local-", localSecretsService.prefix);
    assertEquals(serviceEndpoint, "https://mock-secrets-endpoint");
  }
}
