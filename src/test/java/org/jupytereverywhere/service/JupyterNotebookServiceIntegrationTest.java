package org.jupytereverywhere.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookRetrieved;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.jupytereverywhere.repository.JupyterNotebookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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

  @MockitoSpyBean private JupyterNotebookRepository notebookRepository;

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

  @Test
  void testDeleteNotebook_ByUUID_FullFlow() throws IOException {
    String notebookJson = Files.readString(Path.of("scripts/example-notebooks/py.ipynb"));
    JupyterNotebookDTO notebook = objectMapper.readValue(notebookJson, JupyterNotebookDTO.class);

    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebook);
    request.setPassword("");

    UUID sessionId = UUID.randomUUID();
    JupyterNotebookSaved saved =
        notebookService.uploadNotebook(request, sessionId, "test.example.com", notebookJson);

    assertNotNull(saved.getId());
    assertNotNull(notebookService.getNotebookContent(saved.getId()));

    notebookService.deleteNotebook(saved.getId(), "integration-test");

    assertThrows(
        NotebookNotFoundException.class, () -> notebookService.getNotebookContent(saved.getId()));
  }

  @Test
  void testDeleteNotebook_ByReadableId_FullFlow() throws IOException {
    String notebookJson = Files.readString(Path.of("scripts/example-notebooks/py.ipynb"));
    JupyterNotebookDTO notebook = objectMapper.readValue(notebookJson, JupyterNotebookDTO.class);

    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebook);
    request.setPassword("");

    UUID sessionId = UUID.randomUUID();
    JupyterNotebookSaved saved =
        notebookService.uploadNotebook(request, sessionId, "test.example.com", notebookJson);

    String readableId = saved.getReadableId();
    assertNotNull(readableId);
    assertNotNull(notebookService.getNotebookContent(readableId));

    notebookService.deleteNotebookByReadableId(readableId, "integration-test");

    assertThrows(
        NotebookNotFoundException.class, () -> notebookService.getNotebookContent(saved.getId()));
  }

  @Test
  void testDeleteNotebooksBySessionId_FullFlow() throws IOException {
    String notebookJson = Files.readString(Path.of("scripts/example-notebooks/py.ipynb"));
    JupyterNotebookDTO notebook = objectMapper.readValue(notebookJson, JupyterNotebookDTO.class);

    UUID sessionId = UUID.randomUUID();

    // Create multiple notebooks under the same session
    JupyterNotebookRequest request1 = new JupyterNotebookRequest();
    request1.setNotebook(notebook);
    request1.setPassword("");
    JupyterNotebookSaved saved1 =
        notebookService.uploadNotebook(request1, sessionId, "test.example.com", notebookJson);

    JupyterNotebookRequest request2 = new JupyterNotebookRequest();
    request2.setNotebook(notebook);
    request2.setPassword("");
    JupyterNotebookSaved saved2 =
        notebookService.uploadNotebook(request2, sessionId, "test.example.com", notebookJson);

    // Verify both exist
    assertNotNull(notebookService.getNotebookContent(saved1.getId()));
    assertNotNull(notebookService.getNotebookContent(saved2.getId()));

    // Delete all notebooks for the session
    int deletedCount = notebookService.deleteNotebooksBySessionId(sessionId, "integration-test");

    assertEquals(2, deletedCount);

    // Verify both are gone
    assertThrows(
        NotebookNotFoundException.class, () -> notebookService.getNotebookContent(saved1.getId()));
    assertThrows(
        NotebookNotFoundException.class, () -> notebookService.getNotebookContent(saved2.getId()));

    // Verify metadata rows are removed
    assertTrue(notebookRepository.findBySessionId(sessionId).isEmpty());
  }

  @Test
  void testDeleteNotebooksBySessionId_EmptySession_ReturnsZero() {
    UUID emptySessionId = UUID.randomUUID();

    int deletedCount =
        notebookService.deleteNotebooksBySessionId(emptySessionId, "integration-test");

    assertEquals(0, deletedCount);
  }

  @Test
  void testDeleteNotebook_ReadableIdRemainsConsumed() throws IOException {
    String notebookJson = Files.readString(Path.of("scripts/example-notebooks/py.ipynb"));
    JupyterNotebookDTO notebook = objectMapper.readValue(notebookJson, JupyterNotebookDTO.class);

    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebook);
    request.setPassword("");

    UUID sessionId = UUID.randomUUID();
    JupyterNotebookSaved saved =
        notebookService.uploadNotebook(request, sessionId, "test.example.com", notebookJson);

    String deletedReadableId = saved.getReadableId();
    notebookService.deleteNotebook(saved.getId(), "integration-test");

    // Create another notebook and verify it gets a different readable ID
    JupyterNotebookRequest request2 = new JupyterNotebookRequest();
    request2.setNotebook(notebook);
    request2.setPassword("");
    JupyterNotebookSaved saved2 =
        notebookService.uploadNotebook(
            request2, UUID.randomUUID(), "test.example.com", notebookJson);

    assertNotEquals(
        deletedReadableId,
        saved2.getReadableId(),
        "Deleted notebook's readable ID should not be reused");
  }

  @Test
  void testDeleteNotebook_DBFailure_StorageFileAndMetadataPreserved() throws IOException {
    String notebookJson = Files.readString(Path.of("scripts/example-notebooks/py.ipynb"));
    JupyterNotebookDTO notebook = objectMapper.readValue(notebookJson, JupyterNotebookDTO.class);

    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebook);
    request.setPassword("");

    UUID sessionId = UUID.randomUUID();
    JupyterNotebookSaved saved =
        notebookService.uploadNotebook(request, sessionId, "test.example.com", notebookJson);

    // Verify notebook exists
    assertNotNull(notebookService.getNotebookContent(saved.getId()));

    // Simulate DB failure on deleteById
    doThrow(new RuntimeException("Simulated DB failure"))
        .when(notebookRepository)
        .deleteById(saved.getId());

    try {
      // Attempt to delete — should fail due to DB error
      assertThrows(
          RuntimeException.class,
          () -> notebookService.deleteNotebook(saved.getId(), "integration-test"));

      // Verify both metadata and storage file still exist (consistency guarantee)
      JupyterNotebookRetrieved retrieved = notebookService.getNotebookContent(saved.getId());
      assertNotNull(retrieved, "Metadata should still exist after DB failure");
      assertNotNull(
          retrieved.getNotebookContent(), "Storage file should still exist after DB failure");
    } finally {
      reset(notebookRepository);
    }
  }

  @Test
  void testDeleteNotebooksBySessionId_DBFailure_AllDataPreserved() throws IOException {
    String notebookJson = Files.readString(Path.of("scripts/example-notebooks/py.ipynb"));
    JupyterNotebookDTO notebook = objectMapper.readValue(notebookJson, JupyterNotebookDTO.class);

    UUID sessionId = UUID.randomUUID();

    // Create multiple notebooks under the same session
    JupyterNotebookRequest request1 = new JupyterNotebookRequest();
    request1.setNotebook(notebook);
    request1.setPassword("");
    JupyterNotebookSaved saved1 =
        notebookService.uploadNotebook(request1, sessionId, "test.example.com", notebookJson);

    JupyterNotebookRequest request2 = new JupyterNotebookRequest();
    request2.setNotebook(notebook);
    request2.setPassword("");
    JupyterNotebookSaved saved2 =
        notebookService.uploadNotebook(request2, sessionId, "test.example.com", notebookJson);

    // Verify both exist
    assertNotNull(notebookService.getNotebookContent(saved1.getId()));
    assertNotNull(notebookService.getNotebookContent(saved2.getId()));

    // Simulate DB failure on batch delete
    doThrow(new RuntimeException("Simulated DB failure"))
        .when(notebookRepository)
        .deleteAllInBatch(any());

    try {
      // Attempt to delete — should fail due to DB error
      assertThrows(
          RuntimeException.class,
          () -> notebookService.deleteNotebooksBySessionId(sessionId, "integration-test"));

      // Verify all metadata and storage files still exist (consistency guarantee)
      assertNotNull(
          notebookService.getNotebookContent(saved1.getId()),
          "First notebook should still exist after DB failure");
      assertNotNull(
          notebookService.getNotebookContent(saved2.getId()),
          "Second notebook should still exist after DB failure");
      assertEquals(
          2,
          notebookRepository.findBySessionId(sessionId).size(),
          "All metadata rows should remain intact");
    } finally {
      reset(notebookRepository);
    }
  }
}
