package org.coursekata.service.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.coursekata.dto.JupyterNotebookDTO;
import org.coursekata.exception.S3DownloadException;
import org.coursekata.service.aws.secrets.SecretsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

  @Mock
  private SecretsService secretsService;

  @Mock
  private S3Client s3Client;

  @InjectMocks
  private S3StorageService s3StorageService;

  private Map<String, String> secretValues;
  private String notebookJson;
  private String fileName;
  private String bucketName;

  @BeforeEach
  void setUp() {

    ReflectionTestUtils.setField(s3StorageService, "region", "us-west-1");

    notebookJson = "{ \"notebook\": true }";
    fileName = "notebook.json";
    bucketName = "test-bucket";

    secretValues = new HashMap<>();
    secretValues.put("access_key", "test-access-key");
    secretValues.put("secret_key", "test-secret-key");
    secretValues.put("url", bucketName);

    when(secretsService.getSecretValues("jupyter-s3")).thenReturn(secretValues);
    s3StorageService.initializeS3Client();

    try {
      java.lang.reflect.Field s3ClientField = s3StorageService.getClass().getDeclaredField("s3Client");
      s3ClientField.setAccessible(true);
      s3ClientField.set(s3StorageService, s3Client);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testDownloadNotebook_Success() {
    String notebookJson = "{\"nbformat\":4,\"nbformat_minor\":2,\"metadata\":{\"kernelspec\":{\"name\":\"python3\",\"display_name\":\"Python 3\"},\"language_info\":{\"name\":\"python\",\"version\":\"3.8.5\"}},\"cells\":[]}";

    InputStream inputStream = new ByteArrayInputStream(notebookJson.getBytes(StandardCharsets.UTF_8));
    GetObjectResponse getObjectResponse = GetObjectResponse.builder().build();
    ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
        getObjectResponse,
        AbortableInputStream.create(inputStream)
    );

    when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

    JupyterNotebookDTO result = s3StorageService.downloadNotebook(fileName);

    assertNotNull(result);
    assertEquals(4, result.getNbformat());
    assertEquals(2, result.getNbformatMinor());
    assertNotNull(result.getMetadata());
    assertEquals("Python 3", result.getMetadata().getKernelspec().getDisplayName());
    assertEquals("python3", result.getMetadata().getKernelspec().getName());
    assertEquals("python", result.getMetadata().getLanguageInfo().getName());
    assertEquals("3.8.5", result.getMetadata().getLanguageInfo().getVersion());
    assertNotNull(result.getCells());
    assertTrue(result.getCells().isEmpty());  // Verifica que la lista de celdas está vacía
  }

  @Test
  void testDownloadNotebook_S3DownloadException() {
    String fileName = "non-existent-notebook.ipynb";

    when(s3Client.getObject(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().message("File not found").build());

    Exception exception = assertThrows(S3DownloadException.class, () -> {
      s3StorageService.downloadNotebook(fileName);
    });

    String expectedMessage = "Error downloading notebook from S3";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }
}
