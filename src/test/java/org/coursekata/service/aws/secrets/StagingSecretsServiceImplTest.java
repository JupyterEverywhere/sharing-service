package org.coursekata.service.aws.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StagingSecretsServiceImplTest {

  @Mock
  private AWSSecretsManager secretsManager;

  @InjectMocks
  private StagingSecretsServiceImpl stagingSecretsService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    stagingSecretsService = new StagingSecretsServiceImpl();
    stagingSecretsService.secretsManager = secretsManager;  // Inyectamos el mock
  }

  @Test
  void testGetSecretValues_Success() {
    GetSecretValueResult mockResult = new GetSecretValueResult();
    mockResult.setSecretString("{\"access_key\":\"mockAccessKey\",\"secret_key\":\"mockSecretKey\"}");

    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(mockResult);

    Map<String, String> expectedValues = new HashMap<>();
    expectedValues.put("access_key", "mockAccessKey");
    expectedValues.put("secret_key", "mockSecretKey");

    Map<String, String> actualValues = stagingSecretsService.getSecretValues("test-secret");

    assertEquals(expectedValues, actualValues);
  }

  @Test
  void testGetSecretValues_EmptySecret() {
    GetSecretValueResult mockResult = new GetSecretValueResult();
    mockResult.setSecretString("{}");

    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(mockResult);

    Map<String, String> expectedValues = new HashMap<>();
    Map<String, String> actualValues = stagingSecretsService.getSecretValues("test-secret");

    assertEquals(expectedValues, actualValues);
  }
}
