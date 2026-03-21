package org.jupytereverywhere.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class DatabaseCredentialsEnvironmentPostProcessorTest {

  private DatabaseCredentialsEnvironmentPostProcessor processor;
  private MockEnvironment environment;
  private SpringApplication application;

  @BeforeEach
  void setUp() {
    processor = new DatabaseCredentialsEnvironmentPostProcessor();
    environment = new MockEnvironment();
    application = new SpringApplication();
  }

  @AfterEach
  void tearDown() {
    // Clean up system properties used by the post-processor
    System.clearProperty("DB_CREDENTIALS");
    System.clearProperty("DB_USERNAME");
    System.clearProperty("DB_PASSWORD");
    System.clearProperty("DB_IAM_AUTH");
    System.clearProperty("DB_IAM_USERNAME");
    System.clearProperty("DB_IAM_ADMIN_SECRET");
    System.clearProperty("DB_HOST");
    System.clearProperty("DB_PORT");
    System.clearProperty("DB_NAME");
    System.clearProperty("AWS_REGION");
  }

  // --- Mode 1: Static JSON ---

  @Test
  void staticJsonMode_resolvesCorrectProperties() {
    System.setProperty("DB_CREDENTIALS", "{\"username\":\"jsonuser\",\"password\":\"jsonpass\"}");

    processor.postProcessEnvironment(environment, application);

    assertEquals("jsonuser", environment.getProperty("spring.datasource.username"));
    assertEquals("jsonpass", environment.getProperty("spring.datasource.password"));
  }

  @Test
  void staticJsonMode_doesNotSetFlywayProperties() {
    System.setProperty("DB_CREDENTIALS", "{\"username\":\"jsonuser\",\"password\":\"jsonpass\"}");

    processor.postProcessEnvironment(environment, application);

    assertNull(environment.getProperty("spring.flyway.url"));
    assertNull(environment.getProperty("spring.flyway.user"));
    assertNull(environment.getProperty("spring.flyway.password"));
  }

  @Test
  void staticJsonMode_invalidJson_throwsIllegalState() {
    System.setProperty("DB_CREDENTIALS", "not-json");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("DB_CREDENTIALS"));
  }

  // --- Mode 2: Static Separate ---

  @Test
  void staticSeparateMode_resolvesCorrectProperties() {
    System.setProperty("DB_USERNAME", "sepuser");
    System.setProperty("DB_PASSWORD", "seppass");

    processor.postProcessEnvironment(environment, application);

    assertEquals("sepuser", environment.getProperty("spring.datasource.username"));
    assertEquals("seppass", environment.getProperty("spring.datasource.password"));
  }

  @Test
  void staticSeparateMode_doesNotSetFlywayProperties() {
    System.setProperty("DB_USERNAME", "sepuser");
    System.setProperty("DB_PASSWORD", "seppass");

    processor.postProcessEnvironment(environment, application);

    assertNull(environment.getProperty("spring.flyway.url"));
    assertNull(environment.getProperty("spring.flyway.user"));
    assertNull(environment.getProperty("spring.flyway.password"));
  }

  // --- Mode 3: IAM ---

  @Test
  void iamMode_setsAllExpectedDataSourceProperties() throws Exception {
    setIamSystemProperties();

    DatabaseCredentialsEnvironmentPostProcessor spyProcessor = spy(processor);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode adminCreds = mapper.readTree("{\"username\":\"admin\",\"password\":\"adminpass\"}");
    doReturn(adminCreds).when(spyProcessor).retrieveSecret(anyString(), anyString());

    spyProcessor.postProcessEnvironment(environment, application);

    assertEquals(
        "jdbc:aws-wrapper:postgresql://mydb.rds.amazonaws.com:5432/sharingservice",
        environment.getProperty("spring.datasource.url"));
    assertEquals(
        "software.amazon.jdbc.Driver",
        environment.getProperty("spring.datasource.driver-class-name"));
    assertEquals("app_user", environment.getProperty("spring.datasource.username"));
    assertEquals(
        "iam",
        environment.getProperty("spring.datasource.hikari.data-source-properties.wrapperPlugins"));
    assertEquals(
        "us-east-1",
        environment.getProperty("spring.datasource.hikari.data-source-properties.iamRegion"));
    assertEquals(
        "software.amazon.jdbc.util.HikariCPSQLException",
        environment.getProperty("spring.datasource.hikari.exception-override-class-name"));
  }

  @Test
  void iamMode_setsAllExpectedFlywayProperties() throws Exception {
    setIamSystemProperties();

    DatabaseCredentialsEnvironmentPostProcessor spyProcessor = spy(processor);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode adminCreds = mapper.readTree("{\"username\":\"admin\",\"password\":\"adminpass\"}");
    doReturn(adminCreds).when(spyProcessor).retrieveSecret(anyString(), anyString());

    spyProcessor.postProcessEnvironment(environment, application);

    assertEquals(
        "jdbc:postgresql://mydb.rds.amazonaws.com:5432/sharingservice",
        environment.getProperty("spring.flyway.url"));
    assertEquals("admin", environment.getProperty("spring.flyway.user"));
    assertEquals("adminpass", environment.getProperty("spring.flyway.password"));
  }

  @Test
  void iamMode_usesDefaultPortAndName() throws Exception {
    System.setProperty("DB_IAM_AUTH", "true");
    System.setProperty("DB_IAM_USERNAME", "app_user");
    System.setProperty("DB_IAM_ADMIN_SECRET", "prod/db-admin");
    System.setProperty("DB_HOST", "mydb.rds.amazonaws.com");
    System.setProperty("AWS_REGION", "us-east-1");
    // DB_PORT and DB_NAME intentionally not set

    DatabaseCredentialsEnvironmentPostProcessor spyProcessor = spy(processor);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode adminCreds = mapper.readTree("{\"username\":\"admin\",\"password\":\"pass\"}");
    doReturn(adminCreds).when(spyProcessor).retrieveSecret(anyString(), anyString());

    spyProcessor.postProcessEnvironment(environment, application);

    assertEquals(
        "jdbc:aws-wrapper:postgresql://mydb.rds.amazonaws.com:5432/sharingservice",
        environment.getProperty("spring.datasource.url"));
  }

  @Test
  void iamMode_missingRequiredVars_throwsIllegalState() {
    System.setProperty("DB_IAM_AUTH", "true");
    // Missing all required IAM vars

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("DB_IAM_USERNAME"));
    assertTrue(ex.getMessage().contains("DB_IAM_ADMIN_SECRET"));
    assertTrue(ex.getMessage().contains("DB_HOST"));
    assertTrue(ex.getMessage().contains("AWS_REGION"));
  }

  @Test
  void iamMode_unreachableSecretsManager_throwsRuntimeException() {
    setIamSystemProperties();

    DatabaseCredentialsEnvironmentPostProcessor spyProcessor = spy(processor);
    doThrow(new RuntimeException("Failed to retrieve secret: prod/db-admin"))
        .when(spyProcessor)
        .retrieveSecret(anyString(), anyString());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> spyProcessor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("Secrets Manager"));
  }

  // --- Mutual exclusivity ---

  @Test
  void conflictingModes_credentialsJsonAndIam_throwsIllegalState() {
    System.setProperty("DB_CREDENTIALS", "{\"username\":\"u\",\"password\":\"p\"}");
    System.setProperty("DB_IAM_AUTH", "true");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("multiple credential modes"));
  }

  @Test
  void conflictingModes_usernamePasswordAndIam_throwsIllegalState() {
    System.setProperty("DB_USERNAME", "user");
    System.setProperty("DB_PASSWORD", "pass");
    System.setProperty("DB_IAM_AUTH", "true");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("multiple credential modes"));
  }

  @Test
  void conflictingModes_allThreeSet_throwsIllegalState() {
    System.setProperty("DB_CREDENTIALS", "{\"username\":\"u\",\"password\":\"p\"}");
    System.setProperty("DB_USERNAME", "user");
    System.setProperty("DB_PASSWORD", "pass");
    System.setProperty("DB_IAM_AUTH", "true");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("multiple credential modes"));
  }

  @Test
  void noModeConfigured_throwsIllegalState() {
    // No properties set at all

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("must be set"));
  }

  @Test
  void usernameWithoutPassword_throwsIllegalState() {
    System.setProperty("DB_USERNAME", "user");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("DB_PASSWORD is missing"));
  }

  @Test
  void passwordWithoutUsername_throwsIllegalState() {
    System.setProperty("DB_PASSWORD", "pass");

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> processor.postProcessEnvironment(environment, application));
    assertTrue(ex.getMessage().contains("DB_USERNAME is missing"));
  }

  private void setIamSystemProperties() {
    System.setProperty("DB_IAM_AUTH", "true");
    System.setProperty("DB_IAM_USERNAME", "app_user");
    System.setProperty("DB_IAM_ADMIN_SECRET", "prod/db-admin");
    System.setProperty("DB_HOST", "mydb.rds.amazonaws.com");
    System.setProperty("DB_PORT", "5432");
    System.setProperty("DB_NAME", "sharingservice");
    System.setProperty("AWS_REGION", "us-east-1");
  }
}
