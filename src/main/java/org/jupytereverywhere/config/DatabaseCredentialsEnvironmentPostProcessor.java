package org.jupytereverywhere.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class DatabaseCredentialsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final Logger log =
      LoggerFactory.getLogger(DatabaseCredentialsEnvironmentPostProcessor.class);

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String dbCredentialsJson = getEnvOrProperty("DB_CREDENTIALS");
    String dbUsername = getEnvOrProperty("DB_USERNAME");
    String dbPassword = getEnvOrProperty("DB_PASSWORD");
    String dbIamAuth = getEnvOrProperty("DB_IAM_AUTH");

    boolean hasCredentialsJson = isPresent(dbCredentialsJson);
    boolean hasUsername = isPresent(dbUsername);
    boolean hasPassword = isPresent(dbPassword);
    boolean hasIamAuth = "true".equalsIgnoreCase(dbIamAuth);

    validateMutualExclusivity(hasCredentialsJson, hasUsername, hasPassword, hasIamAuth);

    Map<String, Object> propertyMap = new HashMap<>();

    if (hasIamAuth) {
      configureIamMode(propertyMap);
    } else if (hasCredentialsJson) {
      configureStaticJsonMode(dbCredentialsJson, propertyMap);
    } else if (hasUsername && hasPassword) {
      configureStaticSeparateMode(dbUsername, dbPassword, propertyMap);
    } else {
      throw new IllegalStateException(
          "Database credential configuration is invalid: "
              + "one of DB_CREDENTIALS, DB_USERNAME+DB_PASSWORD, or DB_IAM_AUTH=true must be set.");
    }

    if (!propertyMap.isEmpty()) {
      environment
          .getPropertySources()
          .addFirst(new MapPropertySource("dbCredentialsOverride", propertyMap));
    }
  }

  private void validateMutualExclusivity(
      boolean hasCredentialsJson, boolean hasUsername, boolean hasPassword, boolean hasIamAuth) {
    List<String> activeModes = new ArrayList<>();
    if (hasCredentialsJson) {
      activeModes.add("DB_CREDENTIALS");
    }
    if (hasUsername || hasPassword) {
      activeModes.add("DB_USERNAME/DB_PASSWORD");
    }
    if (hasIamAuth) {
      activeModes.add("DB_IAM_AUTH");
    }

    if (activeModes.size() > 1) {
      throw new IllegalStateException(
          "Database credential configuration is invalid: "
              + "multiple credential modes detected ("
              + String.join(", ", activeModes)
              + "). Only one mode may be active.");
    }

    if (hasUsername && !hasPassword) {
      throw new IllegalStateException(
          "Database credential configuration is invalid: "
              + "DB_USERNAME is set but DB_PASSWORD is missing.");
    }
    if (!hasUsername && hasPassword) {
      throw new IllegalStateException(
          "Database credential configuration is invalid: "
              + "DB_PASSWORD is set but DB_USERNAME is missing.");
    }
  }

  private void configureIamMode(Map<String, Object> propertyMap) {
    log.info("Database credential mode selected: {}", "IAM");

    String iamUsername = getEnvOrProperty("DB_IAM_USERNAME");
    String adminSecret = getEnvOrProperty("DB_IAM_ADMIN_SECRET");
    String dbHost = getEnvOrProperty("DB_HOST");
    String dbPort = getEnvOrProperty("DB_PORT");
    String dbName = getEnvOrProperty("DB_NAME");
    String awsRegion = getEnvOrProperty("AWS_REGION");

    List<String> missingVars = new ArrayList<>();
    if (!isPresent(iamUsername)) missingVars.add("DB_IAM_USERNAME");
    if (!isPresent(adminSecret)) missingVars.add("DB_IAM_ADMIN_SECRET");
    if (!isPresent(dbHost)) missingVars.add("DB_HOST");
    if (!isPresent(awsRegion)) missingVars.add("AWS_REGION");

    if (!missingVars.isEmpty()) {
      String reason =
          "IAM mode requires the following variables: " + String.join(", ", missingVars);
      log.error("Database credential configuration is invalid: {}", reason);
      throw new IllegalStateException("Database credential configuration is invalid: " + reason);
    }

    if (!isPresent(dbPort)) {
      dbPort = "5432";
    }
    if (!isPresent(dbName)) {
      dbName = "sharingservice";
    }

    // Runtime DataSource: AWS JDBC Wrapper with IAM plugin
    String wrapperUrl = "jdbc:aws-wrapper:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
    propertyMap.put("spring.datasource.url", wrapperUrl);
    propertyMap.put("spring.datasource.driver-class-name", "software.amazon.jdbc.Driver");
    propertyMap.put("spring.datasource.username", iamUsername);
    propertyMap.put("spring.datasource.hikari.data-source-properties.wrapperPlugins", "iam");
    propertyMap.put("spring.datasource.hikari.data-source-properties.iamRegion", awsRegion);
    propertyMap.put(
        "spring.datasource.hikari.exception-override-class-name",
        "software.amazon.jdbc.util.HikariCPSQLException");

    // Flyway admin DataSource: standard PostgreSQL with Secrets Manager credentials
    configureFlywayAdminCredentials(adminSecret, dbHost, dbPort, dbName, awsRegion, propertyMap);
  }

  private void configureFlywayAdminCredentials(
      String secretName,
      String dbHost,
      String dbPort,
      String dbName,
      String awsRegion,
      Map<String, Object> propertyMap) {
    try {
      JsonNode adminCreds = retrieveSecret(secretName, awsRegion);
      String adminUsername = adminCreds.get("username").asText();
      String adminPassword = adminCreds.get("password").asText();

      String flywayUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
      propertyMap.put("spring.flyway.url", flywayUrl);
      propertyMap.put("spring.flyway.user", adminUsername);
      propertyMap.put("spring.flyway.password", adminPassword);

      log.info("Admin credentials retrieved from Secrets Manager, secretName={}", secretName);
    } catch (Exception e) {
      log.error(
          "Failed to retrieve admin credentials from Secrets Manager, secretName={}, error={}",
          secretName,
          e.getMessage());
      throw new RuntimeException(
          "Failed to retrieve admin credentials from Secrets Manager: " + secretName, e);
    }
  }

  JsonNode retrieveSecret(String secretName, String awsRegion) {
    try (SecretsManagerClient client =
        SecretsManagerClient.builder().region(Region.of(awsRegion)).build()) {
      GetSecretValueResponse response =
          client.getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build());
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(response.secretString());
    } catch (Exception e) {
      throw new RuntimeException("Failed to retrieve secret: " + secretName, e);
    }
  }

  private void configureStaticJsonMode(String dbCredentialsJson, Map<String, Object> propertyMap) {
    log.info("Database credential mode selected: {}", "STATIC_JSON");

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(dbCredentialsJson);
      String username = node.has("username") ? node.get("username").asText() : null;
      String password = node.has("password") ? node.get("password").asText() : null;
      if (username != null) {
        propertyMap.put("DB_USERNAME", username);
        propertyMap.put("spring.datasource.username", username);
      }
      if (password != null) {
        propertyMap.put("DB_PASSWORD", password);
        propertyMap.put("spring.datasource.password", password);
      }
    } catch (Exception e) {
      log.error("Database credential configuration is invalid: {}", e.getMessage());
      throw new IllegalStateException(
          "DB_CREDENTIALS is set but could not be parsed as valid JSON with 'username' and"
              + " 'password' fields.",
          e);
    }
  }

  private void configureStaticSeparateMode(
      String dbUsername, String dbPassword, Map<String, Object> propertyMap) {
    log.info("Database credential mode selected: {}", "STATIC_SEPARATE");

    propertyMap.put("DB_USERNAME", dbUsername);
    propertyMap.put("spring.datasource.username", dbUsername);
    propertyMap.put("DB_PASSWORD", dbPassword);
    propertyMap.put("spring.datasource.password", dbPassword);
  }

  private static String getEnvOrProperty(String name) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value;
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isEmpty();
  }
}
