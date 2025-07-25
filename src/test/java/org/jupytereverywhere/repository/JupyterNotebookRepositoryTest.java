package org.jupytereverywhere.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.jupytereverywhere.model.JupyterNotebookEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class JupyterNotebookRepositoryTest {

  @SuppressWarnings("resource") // Testcontainers will manage the lifecycle of the container
  @Container
  private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
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

  @Autowired
  private JupyterNotebookRepository notebookRepository;

  @Test
  void testSaveNotebook() {
    JupyterNotebookEntity notebook = createNotebook("s3://bucket/notebook1.ipynb");
    JupyterNotebookEntity savedNotebook = notebookRepository.save(notebook);

    assertNotNull(savedNotebook.getId(), "Saved notebook ID should not be null");
    assertEquals(notebook.getSessionId(), savedNotebook.getSessionId(), "Session IDs should match");
    assertEquals(notebook.getDomain(), savedNotebook.getDomain(), "Domains should match");
    assertEquals(notebook.getReadableId(), savedNotebook.getReadableId(), "Readable ID should match");
  }

  @Test
  void testFindById() {
    JupyterNotebookEntity notebook = createNotebook("s3://bucket/notebook2.ipynb");
    JupyterNotebookEntity savedNotebook = notebookRepository.save(notebook);
    UUID notebookId = savedNotebook.getId();

    Optional<JupyterNotebookEntity> foundNotebook = notebookRepository.findById(notebookId);

    assertTrue(foundNotebook.isPresent(), "Notebook should be found by ID");
    assertEquals(notebookId, foundNotebook.get().getId(), "Notebook IDs should match");
    assertEquals(notebook.getDomain(), foundNotebook.get().getDomain(), "Domains should match");
  }

  @Test
  void testFindByReadableId() {
    // For testing purposes, we'll set a readable ID manually to test the findByReadableId functionality
    // In production, this would be set by the system/trigger, but for unit testing we need predictable values
    String testReadableId = "test-readable-id-for-testing";

    JupyterNotebookEntity notebook = createNotebook("s3://bucket/notebook-readable-test.ipynb", UUID.randomUUID());
    notebook.setReadableId(testReadableId);

    JupyterNotebookEntity savedNotebook = notebookRepository.save(notebook);
    notebookRepository.flush(); // Force database write

    Optional<JupyterNotebookEntity> foundNotebook = notebookRepository.findByReadableId(testReadableId);

    assertTrue(foundNotebook.isPresent(), "Notebook should be found by readable id");
    assertEquals(testReadableId, foundNotebook.get().getReadableId(), "Readable ID should match");
    assertEquals(savedNotebook.getId(), foundNotebook.get().getId(), "Notebook IDs should match");
  }

  @Test
  void testDeleteNotebook() {
    JupyterNotebookEntity notebook = createNotebook("s3://bucket/notebook3.ipynb");
    JupyterNotebookEntity savedNotebook = notebookRepository.save(notebook);
    UUID notebookId = savedNotebook.getId();

    notebookRepository.deleteById(notebookId);

    Optional<JupyterNotebookEntity> deletedNotebook = notebookRepository.findById(notebookId);
    assertFalse(deletedNotebook.isPresent(), "Notebook should be deleted and not found");
  }

  @Test
  void testFindAllNotebooks() {
    JupyterNotebookEntity notebook1 = createNotebook("s3://bucket/notebook4.ipynb");
    JupyterNotebookEntity notebook2 = createNotebook("s3://bucket/notebook5.ipynb");

    notebookRepository.save(notebook1);
    notebookRepository.save(notebook2);

    List<JupyterNotebookEntity> notebooks = notebookRepository.findAll();

    assertEquals(2, notebooks.size(), "Should find 2 notebooks");
  }

  @Test
  void testUpdateNotebook() {
    JupyterNotebookEntity notebook = createNotebook("s3://bucket/notebook6.ipynb");
    JupyterNotebookEntity savedNotebook = notebookRepository.save(notebook);

    savedNotebook.setKernelName("python3.9");
    savedNotebook.setLanguageVersion("3.9");
    savedNotebook.setDomain("updated.com");
    notebookRepository.save(savedNotebook);

    Optional<JupyterNotebookEntity> updatedNotebook = notebookRepository.findById(savedNotebook.getId());

    assertTrue(updatedNotebook.isPresent(), "Updated notebook should be found");
    assertEquals("python3.9", updatedNotebook.get().getKernelName(), "Kernel name should be updated");
    assertEquals("3.9", updatedNotebook.get().getLanguageVersion(), "Language version should be updated");
    assertEquals("updated.com", updatedNotebook.get().getDomain(), "Domain should be updated");
  }

  @Test
  void testFindBySessionId() {
    UUID sessionId = UUID.randomUUID();
    JupyterNotebookEntity notebook1 = createNotebook("s3://bucket/notebook7.ipynb", sessionId);
    JupyterNotebookEntity notebook2 = createNotebook("s3://bucket/notebook8.ipynb", sessionId);

    notebookRepository.save(notebook1);
    notebookRepository.save(notebook2);

    List<JupyterNotebookEntity> notebooks = notebookRepository.findBySessionId(sessionId);

    assertEquals(2, notebooks.size(), "Should find 2 notebooks with the same session ID");
  }

  private JupyterNotebookEntity createNotebook(String storageUrl) {
    return createNotebook(storageUrl, UUID.randomUUID());
  }

  private JupyterNotebookEntity createNotebook(String storageUrl, UUID sessionId) {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(sessionId);
    notebook.setDomain("example.com");
    notebook.setKernelName("python3");
    notebook.setKernelDisplayName("Python 3");
    notebook.setLanguage("Python");
    notebook.setLanguageVersion("3.8");
    notebook.setFileExtension(".ipynb");
    notebook.setStorageUrl(storageUrl);
    notebook.setCreatedAt(Timestamp.from(Instant.now()));
    // DO NOT set readableId - let the system assign it via trigger
    return notebook;
  }
}
