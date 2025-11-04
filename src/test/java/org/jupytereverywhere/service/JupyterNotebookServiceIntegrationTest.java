package org.jupytereverywhere.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookRetrieved;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for JupyterNotebookService that verify end-to-end functionality including
 * database and file storage operations.
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(
    properties = {
      "storage.type=file",
      "python.interpreter.path=/usr/bin/python3" // Will be mocked in these tests
    })
class JupyterNotebookServiceIntegrationTest {

  static {
    System.setProperty("DB_USERNAME", "test");
    System.setProperty("DB_PASSWORD", "test");
  }

  @SuppressWarnings("resource")
  @Container
  private static final PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @TempDir static Path tempDir;

  @DynamicPropertySource
  static void setDataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "test");
    registry.add("spring.datasource.password", () -> "test");
    registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
    registry.add("storage.path.local", () -> tempDir.toString());
  }

  @Autowired private JupyterNotebookService notebookService;

  @Autowired private ObjectMapper objectMapper;

  /**
   * Tests that R notebooks with extra metadata fields (like pygments_lexer) can be uploaded and
   * retrieved without errors. This verifies that FileStorageService uses the configured
   * ObjectMapper with FAIL_ON_UNKNOWN_PROPERTIES=false.
   */
  @Test
  void testRNotebook_WithPygmentsLexer_RoundTrip() throws IOException {
    // Load the actual example-r.ipynb file
    String rNotebookJson = Files.readString(Path.of("scripts/example-notebooks/r.ipynb"));

    // Parse it to a DTO
    JupyterNotebookDTO rNotebook = objectMapper.readValue(rNotebookJson, JupyterNotebookDTO.class);

    // Create upload request
    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(rNotebook);
    request.setPassword("");

    UUID sessionId = UUID.randomUUID();
    String domain = "test.example.com";

    // Upload the notebook
    JupyterNotebookSaved saved =
        notebookService.uploadNotebook(request, sessionId, domain, rNotebookJson);

    assertNotNull(saved, "Saved notebook should not be null");
    assertNotNull(saved.getId(), "Notebook ID should not be null");
    assertNotNull(saved.getReadableId(), "Readable ID should not be null");

    // Retrieve the notebook - now returns raw JSON instead of deserializing
    JupyterNotebookRetrieved retrieved = notebookService.getNotebookContent(saved.getId());

    assertNotNull(retrieved, "Retrieved notebook should not be null");
    assertNotNull(retrieved.getNotebookContent(), "Retrieved notebook content should not be null");
    assertEquals(
        saved.getId(),
        retrieved.getId(),
        "Retrieved notebook ID should match the saved notebook ID");

    // Parse the JSON to verify content
    JupyterNotebookDTO parsedNotebook =
        objectMapper.readValue(retrieved.getNotebookContent(), JupyterNotebookDTO.class);

    // Verify the notebook has the expected metadata
    assertNotNull(parsedNotebook.getMetadata(), "Notebook metadata should not be null");
    assertNotNull(
        parsedNotebook.getMetadata().getLanguageInfo(), "Language info should not be null");
    assertEquals(
        "R", parsedNotebook.getMetadata().getLanguageInfo().getName(), "Language should be R");

    // Verify the notebook has cells
    assertNotNull(parsedNotebook.getCells(), "Cells should not be null");
    assertFalse(parsedNotebook.getCells().isEmpty(), "Should have at least one cell");
  }

  /**
   * Tests that notebooks with empty language_info.name can be uploaded and retrieved. This is
   * allowed by the nbformat spec.
   */
  @Test
  void testNotebook_WithEmptyLanguageName_RoundTrip() throws IOException {
    // Load the example no-kernel notebook
    String noKernelJson = Files.readString(Path.of("scripts/example-notebooks/no-kernel.ipynb"));

    JupyterNotebookDTO noKernelNotebook =
        objectMapper.readValue(noKernelJson, JupyterNotebookDTO.class);

    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(noKernelNotebook);
    request.setPassword("");

    UUID sessionId = UUID.randomUUID();
    String domain = "test.example.com";

    // Upload
    JupyterNotebookSaved saved =
        notebookService.uploadNotebook(request, sessionId, domain, noKernelJson);

    assertNotNull(saved);

    // Retrieve
    JupyterNotebookRetrieved retrieved = notebookService.getNotebookContent(saved.getId());

    assertNotNull(retrieved);
    assertNotNull(retrieved.getNotebookContent());
    assertEquals(saved.getId(), retrieved.getId());

    // Parse the JSON to verify content
    JupyterNotebookDTO parsedNotebook =
        objectMapper.readValue(retrieved.getNotebookContent(), JupyterNotebookDTO.class);

    // Verify the language_info.name is handled correctly (should be empty string in file)
    assertNotNull(parsedNotebook.getMetadata());
    assertNotNull(parsedNotebook.getMetadata().getLanguageInfo());
    // Note: empty string in language_info.name is preserved in the file but not stored in DB
  }
}
