package org.jupytereverywhere.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.jupytereverywhere.model.JupyterNotebookEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verification tests for empty notebook support.
 *
 * <p>This test suite verifies that the issues described in empty-notebook-sharing-issue.md have
 * been properly addressed by migrations V5 and V6: - V5__make_language_info_fields_nullable.sql
 * (Sep 12) - Made fileExtension and languageVersion nullable -
 * V6__make_metadata_fields_nullable.sql (Oct 22) - Made language, kernelName, kernelDisplayName
 * nullable
 *
 * <p>All tests should PASS, confirming that empty notebooks are now properly supported at the
 * database level.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class EmptyNotebookValidationVerificationTest {

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

  @DynamicPropertySource
  static void setDataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "test");
    registry.add("spring.datasource.password", () -> "test");
    registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
  }

  @Autowired private JupyterNotebookRepository notebookRepository;

  /**
   * Verifies that empty notebooks (minimal valid per nbformat spec) can now be saved. This was the
   * main issue reported - empty notebooks should be valid.
   */
  @Test
  void testEmptyNotebookCanBeSaved() {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain("example.com");
    notebook.setStorageUrl("s3://test-bucket/empty-notebook.ipynb");
    notebook.setCreatedAt(Timestamp.from(Instant.now()));

    // No metadata fields set - all remain null

    // This should now work thanks to V6 migration
    JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);

    assertNotNull(saved.getId(), "Empty notebook should save successfully");
    assertNull(saved.getKernelName(), "kernelName can be null");
    assertNull(saved.getKernelDisplayName(), "kernelDisplayName can be null");
    assertNull(saved.getLanguage(), "language can be null");
    assertNull(saved.getFileExtension(), "fileExtension can be null");
    assertNull(saved.getLanguageVersion(), "languageVersion can be null");
  }

  /** Verifies that markdown-only notebooks (no kernel needed) can be saved. */
  @Test
  void testMarkdownOnlyNotebookCanBeSaved() {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain("docs.example.com");
    notebook.setStorageUrl("s3://test-bucket/readme.ipynb");
    notebook.setCreatedAt(Timestamp.from(Instant.now()));

    // Markdown notebooks don't need kernel or language info

    JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);

    assertNotNull(saved.getId(), "Markdown-only notebook should save successfully");
    assertNull(saved.getKernelName(), "No kernel needed for markdown");
    assertNull(saved.getLanguage(), "No language needed for markdown");
  }

  /**
   * Verifies that notebooks with partial metadata (only language name) can be saved. Per nbformat
   * spec, only language_info.name is required when language_info is present.
   */
  @Test
  void testPartialMetadataCanBeSaved() {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain("example.com");
    notebook.setStorageUrl("s3://test-bucket/partial.ipynb");
    notebook.setCreatedAt(Timestamp.from(Instant.now()));

    // Only set language name (the only required field in language_info)
    notebook.setLanguage("python");
    // Leave other fields null (optional per spec)

    JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);

    assertNotNull(saved.getId());
    assertEquals("python", saved.getLanguage());
    assertNull(saved.getFileExtension(), "fileExtension is optional");
    assertNull(saved.getLanguageVersion(), "languageVersion is optional");
    assertNull(saved.getKernelName(), "kernelspec is optional");
    assertNull(saved.getKernelDisplayName(), "kernelspec is optional");
  }

  /** Verifies that notebooks with only kernelspec (no language_info) can be saved. */
  @Test
  void testOnlyKernelspecCanBeSaved() {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain("example.com");
    notebook.setStorageUrl("s3://test-bucket/kernel-only.ipynb");
    notebook.setCreatedAt(Timestamp.from(Instant.now()));

    // Set only kernel info
    notebook.setKernelName("python3");
    notebook.setKernelDisplayName("Python 3");
    // language_info fields remain null

    JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);

    assertNotNull(saved.getId());
    assertEquals("python3", saved.getKernelName());
    assertEquals("Python 3", saved.getKernelDisplayName());
    assertNull(saved.getLanguage(), "language_info is optional");
    assertNull(saved.getFileExtension());
    assertNull(saved.getLanguageVersion());
  }

  /** Verifies that fully specified notebooks still work correctly. */
  @Test
  void testFullySpecifiedNotebookStillWorks() {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain("example.com");
    notebook.setStorageUrl("s3://test-bucket/full.ipynb");
    notebook.setCreatedAt(Timestamp.from(Instant.now()));

    // Set all metadata fields
    notebook.setKernelName("python3");
    notebook.setKernelDisplayName("Python 3 (ipykernel)");
    notebook.setLanguage("python");
    notebook.setFileExtension(".py");
    notebook.setLanguageVersion("3.9.0");

    JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);

    assertNotNull(saved.getId());
    assertEquals("python3", saved.getKernelName());
    assertEquals("Python 3 (ipykernel)", saved.getKernelDisplayName());
    assertEquals("python", saved.getLanguage());
    assertEquals(".py", saved.getFileExtension());
    assertEquals("3.9.0", saved.getLanguageVersion());
  }

  /**
   * Documents the fix timeline: - V5 migration (Sep 12): Made fileExtension and languageVersion
   * nullable - V6 migration (Oct 22): Made language, kernelName, kernelDisplayName nullable
   */
  @Test
  void testDocumentFixTimeline() {
    // This test documents that all metadata fields can now be null
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain("timeline.example.com");
    notebook.setStorageUrl("s3://test-bucket/timeline.ipynb");
    notebook.setCreatedAt(Timestamp.from(Instant.now()));

    JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);

    // All metadata fields can now be null
    assertNull(saved.getKernelName(), "Fixed in V6 migration");
    assertNull(saved.getKernelDisplayName(), "Fixed in V6 migration");
    assertNull(saved.getLanguage(), "Fixed in V6 migration");
    assertNull(saved.getFileExtension(), "Fixed in V5 migration");
    assertNull(saved.getLanguageVersion(), "Fixed in V5 migration");

    System.out.println("SUCCESS: All metadata fields are now properly nullable");
    System.out.println("- V5 migration fixed: fileExtension, languageVersion");
    System.out.println("- V6 migration fixed: language, kernelName, kernelDisplayName");
    System.out.println("Empty notebooks are now fully supported at the database level!");
  }
}
