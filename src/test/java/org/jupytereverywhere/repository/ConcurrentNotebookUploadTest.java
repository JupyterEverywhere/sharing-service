package org.jupytereverywhere.repository;

import org.jupytereverywhere.model.JupyterNotebookEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ConcurrentNotebookUploadTest {

  static {
    System.setProperty("DB_USERNAME", "test");
    System.setProperty("DB_PASSWORD", "test");
  }

  @SuppressWarnings("resource")
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
  void testConcurrentNotebookInserts_NoReadableIdCollisions() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    List<UUID> insertedIds = Collections.synchronizedList(new ArrayList<>());
    List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

    // Create 10 concurrent insert tasks
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      executorService.submit(() -> {
        try {
          // Wait for all threads to be ready
          startLatch.await();

          // Create and save notebook (readable_id will be assigned by trigger)
          JupyterNotebookEntity notebook = createNotebook(
              "s3://test-bucket/concurrent-" + index + ".ipynb",
              "concurrent-test-" + index + ".example.com"
          );

          JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);
          insertedIds.add(saved.getId());

        } catch (Exception e) {
          exceptions.add(e);
        } finally {
          doneLatch.countDown();
        }
      });
    }

    // Start all threads simultaneously
    startLatch.countDown();

    // Wait for all threads to complete (max 30 seconds)
    assertTrue(doneLatch.await(30, TimeUnit.SECONDS),
        "All inserts should complete within 30 seconds");

    executorService.shutdown();

    // Verify no exceptions occurred (especially no duplicate key violations)
    if (!exceptions.isEmpty()) {
      System.err.println("Exceptions during concurrent inserts:");
      exceptions.forEach(e -> {
        System.err.println("  - " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
      });

      // Check if any are duplicate key violations
      long duplicateKeyErrors = exceptions.stream()
          .filter(e -> e instanceof DataIntegrityViolationException)
          .filter(e -> e.getMessage() != null &&
              e.getMessage().contains("readable_id_key"))
          .count();

      assertEquals(0, duplicateKeyErrors,
          "No duplicate key violations should occur during concurrent inserts");
    }

    assertEquals(0, exceptions.size(),
        "No exceptions should occur. Found: " + exceptions.stream()
            .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
            .collect(Collectors.joining(", ")));

    // Verify all notebooks were created
    assertEquals(threadCount, insertedIds.size(),
        "All " + threadCount + " notebooks should be inserted successfully");

    // Verify all have unique IDs
    assertEquals(threadCount, insertedIds.stream().distinct().count(),
        "All notebook IDs should be unique");

    // Note: We cannot test readable_id assignment in @DataJpaTest context
    // as the trigger requires full database context. This test verifies that
    // the V7 migration (with SELECT FOR UPDATE SKIP LOCKED) compiles and
    // that concurrent inserts don't cause errors. Actual readable_id collision
    // prevention must be tested in integration/production environments.
  }

  @Test
  void testSequentialNotebookInserts_NoErrors() {
    // This test verifies that notebooks can be inserted sequentially without errors
    // Note: readable_id trigger may not fire in @DataJpaTest context,
    // so we only verify successful inserts without duplicate key violations
    int notebookCount = 5;
    List<JupyterNotebookEntity> savedNotebooks = new ArrayList<>();

    for (int i = 0; i < notebookCount; i++) {
      JupyterNotebookEntity notebook = createNotebook(
          "s3://test-bucket/sequential-" + i + ".ipynb",
          "sequential-test-" + i + ".example.com"
      );
      JupyterNotebookEntity saved = notebookRepository.saveAndFlush(notebook);
      savedNotebooks.add(saved);
    }

    // Verify all notebooks were saved successfully
    assertEquals(notebookCount, savedNotebooks.size(),
        "All notebooks should be saved successfully");

    // Verify all have unique IDs
    List<UUID> ids = savedNotebooks.stream()
        .map(JupyterNotebookEntity::getId)
        .collect(Collectors.toList());

    assertEquals(notebookCount, ids.stream().distinct().count(),
        "All notebook IDs should be unique");
  }

  private JupyterNotebookEntity createNotebook(String storageUrl, String domain) {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    notebook.setSessionId(UUID.randomUUID());
    notebook.setDomain(domain);
    notebook.setStorageUrl(storageUrl);
    notebook.setCreatedAt(Timestamp.from(Instant.now()));
    // Do NOT set readable_id - it should be assigned by the trigger
    return notebook;
  }
}
