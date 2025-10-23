package org.jupytereverywhere.service.aws.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@ExtendWith(MockitoExtension.class)
class ConfigurableSecretsServiceImplTest {

  @Mock private SecretsManagerClient secretsManager;

  private ConfigurableSecretsServiceImpl configurableSecretsService;

  private String prefix = "test-prefix-";

  @BeforeEach
  void setUp() {
    configurableSecretsService = new ConfigurableSecretsServiceImpl(secretsManager, prefix);
  }

  @Test
  void testGetSecretValues_Success() {
    String secretName = "my-secret";
    String secretJson = "{\"foo\":\"bar\", \"baz\":\"qux\"}";

    GetSecretValueResponse response =
        GetSecretValueResponse.builder().secretString(secretJson).build();
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(response);

    Map<String, String> secretValues = configurableSecretsService.getSecretValues(secretName);

    assertEquals(2, secretValues.size());
    assertEquals("bar", secretValues.get("foo"));
    assertEquals("qux", secretValues.get("baz"));
  }

  @Test
  void testGetSecretValues_SecretRetrievalError() {
    String secretName = "my-secret";
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(new RuntimeException("Error retrieving secret"));

    assertThrows(
        RuntimeException.class,
        () -> {
          configurableSecretsService.getSecretValues(secretName);
        });
  }

  @Test
  void testConstructor_Prefix() {
    assertEquals(prefix, configurableSecretsService.prefix);
  }
}
