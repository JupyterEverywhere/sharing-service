package org.jupytereverywhere.service.aws;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.exception.S3DeleteException;
import org.jupytereverywhere.exception.S3DownloadException;
import org.jupytereverywhere.exception.S3UploadException;
import org.jupytereverywhere.service.StorageService;
import org.jupytereverywhere.service.aws.secrets.SecretsService;

@Log4j2
@Service("s3StorageService")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {
  private static final String ACCESS_KEY = "access_key";
  private static final String SECRET_KEY = "secret_key";
  private static final String BUCKET_URL = "url";

  @Value("${aws.s3.region}")
  private String region;

  @Value("${aws.s3.bucket:}")
  private String configuredBucketName;

  // Optional: only used if present
  @Value("${aws.s3.access-key:}")
  private String configuredAccessKey;

  // Optional: only used if present
  @Value("${aws.s3.secret-key:}")
  private String configuredSecretKey;

  private final SecretsService secretsService;
  private S3Client s3Client;
  private String bucketName;
  private String accessKey;
  private String secretKey;


  public S3StorageService(SecretsService secretsService) {
    this.secretsService = secretsService;
  }

  @PostConstruct
  void initializeS3Client() {
    boolean secretLoaded = loadSecretValues();

    if (!secretLoaded) {
      // Use env/properties for all values
      this.accessKey = configuredAccessKey;
      this.secretKey = configuredSecretKey;
      this.bucketName = configuredBucketName;
    }

    // Require bucket name and region
    if (bucketName == null || bucketName.isEmpty()) {
      throw new IllegalStateException("S3 bucket name must be provided via secret or aws.s3.bucket property/env var");
    }
    if (region == null || region.isEmpty()) {
      throw new IllegalStateException("S3 region must be provided via aws.s3.region property/env var");
    }

    // Use explicit credentials if both are present, otherwise use default provider chain
    if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
      AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
      this.s3Client = S3Client.builder()
          .region(Region.of(region))
          .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
          .build();
      log.info("S3 client initialized with explicit credentials from Secrets Manager or properties");
    } else {
      this.s3Client = S3Client.builder()
          .region(Region.of(region))
          .build();
      log.info("S3 client initialized with default AWS credentials provider chain (IAM role, EC2/ECS metadata, etc.)");
    }

    StringMapMessage initLog = new StringMapMessage()
        .with("action", "initializeS3Client")
        .with("status", "success")
        .with("bucketName", bucketName)
        .with("region", region);

    log.info(initLog);
  }

  @Value("${aws.s3.secret-name:jupyter-s3}")
  private String s3SecretName = "jupyter-s3";

  /**
   * Loads secret values if available. Returns true if secret was found and used, false otherwise.
   */
  private boolean loadSecretValues() {
    String effectiveSecretName = (s3SecretName != null) ? s3SecretName : "jupyter-s3";
    Map<String, String> secretValues = null;
    try {
      secretValues = secretsService.getSecretValues(effectiveSecretName);
    } catch (Exception e) {
      log.info("No S3 credentials found in Secrets Manager, will use env/properties for S3 config");
      secretValues = null;
    }

    if (secretValues != null) {
      this.accessKey = secretValues.get(ACCESS_KEY);
      this.secretKey = secretValues.get(SECRET_KEY);
      this.bucketName = secretValues.get(BUCKET_URL);
      StringMapMessage secretLog = new StringMapMessage()
          .with("action", "loadSecretValues")
          .with("secretName", effectiveSecretName)
          .with("accessKey", accessKey != null ? "****" : "N/A")
          .with("secretKey", secretKey != null ? "****" : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A");
      log.info(secretLog);
      return true;
    }
    return false;
  }

  @Override
  public String uploadNotebook(String notebookJson, String fileName) {
    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(fileName)
          .serverSideEncryption("aws:kms")
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromString(notebookJson));

      StringMapMessage successLog = new StringMapMessage()
          .with("action", "uploadNotebook")
          .with("status", "success")
          .with("fileName", fileName != null ? fileName : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A");

      log.info(successLog);

      return fileName;
    } catch (Exception e) {
      StringMapMessage errorLog = new StringMapMessage()
          .with("action", "uploadNotebook")
          .with("status", "failure")
          .with("fileName", fileName != null ? fileName : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A")
          .with("error", e.getMessage() != null ? e.getMessage() : "N/A");

      log.error(errorLog, e);
      throw new S3UploadException("Error uploading notebook to S3", e);
    }
  }

  @Override
  public JupyterNotebookDTO downloadNotebook(String fileName) {
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(fileName)
          .build();

      try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
        String notebookContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        JupyterNotebookDTO notebookDto = objectMapper.readValue(notebookContent, JupyterNotebookDTO.class);

        StringMapMessage successLog = new StringMapMessage()
            .with("action", "downloadNotebook")
            .with("status", "success")
            .with("fileName", fileName != null ? fileName : "N/A")
            .with("bucketName", bucketName != null ? bucketName : "N/A");

        log.info(successLog);

        return notebookDto;
      }
    } catch (IOException e) {
      StringMapMessage ioErrorLog = new StringMapMessage()
          .with("action", "downloadNotebook")
          .with("status", "failure")
          .with("fileName", fileName != null ? fileName : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A")
          .with("error", e.getMessage() != null ? e.getMessage() : "N/A");

      log.error(ioErrorLog, e);
      throw new S3DownloadException("IO Error downloading notebook from S3", e);
    } catch (Exception e) {
      StringMapMessage generalErrorLog = new StringMapMessage()
          .with("action", "downloadNotebook")
          .with("status", "failure")
          .with("fileName", fileName != null ? fileName : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A")
          .with("error", e.getMessage() != null ? e.getMessage() : "N/A");

      log.error(generalErrorLog, e);
      throw new S3DownloadException("Error downloading notebook from S3", e);
    }
  }

  @Override
  public void deleteNotebook(String fileName) {
    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(fileName)
          .build();

      s3Client.deleteObject(deleteObjectRequest);

      StringMapMessage successLog = new StringMapMessage()
          .with("action", "deleteNotebook")
          .with("status", "success")
          .with("fileName", fileName != null ? fileName : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A");

      log.info(successLog);
    } catch (Exception e) {
      StringMapMessage errorLog = new StringMapMessage()
          .with("action", "deleteNotebook")
          .with("status", "failure")
          .with("fileName", fileName != null ? fileName : "N/A")
          .with("bucketName", bucketName != null ? bucketName : "N/A")
          .with("error", e.getMessage() != null ? e.getMessage() : "N/A");

      log.error(errorLog, e);
      throw new S3DeleteException("Error deleting notebook from S3", e);
    }
  }
}
