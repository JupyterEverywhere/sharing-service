package org.jupytereverywhere.service.aws.secrets;

import java.util.Map;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SecretsServiceImplTest {

  @Mock
  private SecretsManagerClient secretsManager;

  @InjectMocks
  private SecretsServiceImpl secretsServiceImpl = new SecretsServiceImpl() {
    @Override
    public Map<String, String> getSecretValues(String secretName) {
      return Map.of();
    }
  };

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    secretsServiceImpl.prefix = "test-";
  }

  @Test
  public void testFetchSecretValues_Success() throws Exception {
    String secretName = "test-secret";
    String secretString = "{\"key1\":\"value1\", \"key2\":\"value2\"}";

    GetSecretValueResponse secretValueResponse = GetSecretValueResponse.builder()
        .secretString(secretString)
        .build();
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResponse);

    Map<String, String> secretValues = secretsServiceImpl.fetchSecretValues(secretName);

    assertNotNull(secretValues);
    assertEquals("value1", secretValues.get("key1"));
    assertEquals("value2", secretValues.get("key2"));
  }

  @Test
  public void testGetSecretNameWithPrefix() {
    String secretNameWithoutPrefix = "my-secret";
    String secretNameWithPrefix = "test-my-secret";

    GetSecretValueResponse secretValueResponse = GetSecretValueResponse.builder()
        .secretString("{\"key1\":\"value1\"}")
        .build();
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResponse);

    assertDoesNotThrow(() -> {
      Map<String, String> secretValues = secretsServiceImpl.fetchSecretValues(secretNameWithoutPrefix);
      assertNotNull(secretValues);
      assertEquals("value1", secretValues.get("key1"));
    });

    assertDoesNotThrow(() -> {
      Map<String, String> secretValues = secretsServiceImpl.fetchSecretValues(secretNameWithPrefix);
      assertNotNull(secretValues);
      assertEquals("value1", secretValues.get("key1"));
    });
  }
}
