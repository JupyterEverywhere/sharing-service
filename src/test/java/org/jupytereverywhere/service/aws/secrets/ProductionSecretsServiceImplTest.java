package org.jupytereverywhere.service.aws.secrets;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.SecretParsingException;
import org.jupytereverywhere.exception.SecretRetrievalException;
import org.jupytereverywhere.service.aws.secrets.ProductionSecretsServiceImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductionSecretsServiceImplTest {

  @Mock
  private AWSSecretsManager secretsManager;

  @InjectMocks
  private ProductionSecretsServiceImpl secretsService;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    secretsService = new ProductionSecretsServiceImpl();
    secretsService.secretsManager = secretsManager; // Set mocked secretsManager
  }

  @Test
  public void testFetchSecretValues_Success() throws Exception {
    String secretName = "test-secret";
    String secretString = "{\"key1\":\"value1\", \"key2\":\"value2\"}";

    // Mock response
    GetSecretValueResult secretValueResult = new GetSecretValueResult().withSecretString(secretString);
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

    // Call method
    Map<String, String> secretValues = secretsService.getSecretValues(secretName);

    // Assert that the values are correct
    assertNotNull(secretValues);
    assertEquals("value1", secretValues.get("key1"));
    assertEquals("value2", secretValues.get("key2"));
  }

  @Test
  public void testFetchSecretValues_SecretRetrievalError() {
    String secretName = "test-secret";

    // Simular excepción de AWSSecretsManager
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(new AWSSecretsManagerException("Error retrieving secret"));

    // Assert que la excepción correcta es lanzada
    SecretRetrievalException exception = assertThrows(SecretRetrievalException.class, () -> {
      secretsService.getSecretValues(secretName);
    });

    // Verificar el mensaje de la excepción
    assertEquals("Error retrieving secret: production-test-secret", exception.getMessage());
  }

  @Test
  public void testFetchSecretValues_SecretParsingError() throws Exception {
    String secretName = "test-secret";
    String invalidJson = "Invalid JSON";

    // Mock response with invalid JSON
    GetSecretValueResult secretValueResult = new GetSecretValueResult().withSecretString(invalidJson);
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

    // Assert exception
    Exception exception = assertThrows(SecretParsingException.class, () -> {
      secretsService.getSecretValues(secretName);
    });

    // Check that the exception message is correct
    assertEquals("Error parsing secret value for: production-test-secret", exception.getMessage());
  }

  @Test
  public void testGetSecretNameWithPrefix() {
    String secretNameWithoutPrefix = "my-secret";
    String secretNameWithPrefix = "production-my-secret";

    // Mocking a valid response for both cases
    GetSecretValueResult secretValueResult = new GetSecretValueResult().withSecretString("{\"key1\":\"value1\"}");
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

    // Test without prefix
    assertDoesNotThrow(() -> {
      Map<String, String> secretValues = secretsService.getSecretValues(secretNameWithoutPrefix);
      assertNotNull(secretValues);
      assertEquals("value1", secretValues.get("key1"));
    });

    // Test with prefix
    assertDoesNotThrow(() -> {
      Map<String, String> secretValues = secretsService.getSecretValues(secretNameWithPrefix);
      assertNotNull(secretValues);
      assertEquals("value1", secretValues.get("key1"));
    });
  }
}
