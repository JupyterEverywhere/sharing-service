package org.jupytereverywhere.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.exception.InvalidNotebookException;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.jupytereverywhere.repository.JupyterNotebookRepository;
import org.jupytereverywhere.service.utils.JupyterNotebookValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test to reproduce the empty notebook validation issues at the service layer. This bypasses
 * HTTP/Controller layers to focus on service validation.
 */
@SpringBootTest
@Testcontainers
class EmptyNotebookValidationReproductionTest {

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

    // Disable JWT for testing
    registry.add("jwt.secret.name", () -> "test-secret");
  }

  @Autowired private JupyterNotebookService notebookService;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private JupyterNotebookValidator validator;

  @Autowired private JupyterNotebookRepository repository;

  @MockitoBean private FileStorageService storageService;

  private UUID sessionId;

  @BeforeEach
  void setUp() {
    sessionId = UUID.randomUUID();

    // Mock S3 storage
    when(storageService.uploadNotebook(anyString(), anyString()))
        .thenReturn("s3://test-bucket/test-notebook.ipynb");

    // downloadNotebook returns a JupyterNotebookDTO
    JupyterNotebookDTO emptyDto = new JupyterNotebookDTO();
    emptyDto.setCells(new java.util.ArrayList<>());
    emptyDto.setMetadata(new org.jupytereverywhere.dto.MetadataDTO());
    emptyDto.setNbformat(4);
    emptyDto.setNbformatMinor(5);
    when(storageService.downloadNotebook(anyString())).thenReturn(emptyDto);
  }

  /**
   * Test Case 1: Empty Notebook with minimal metadata
   *
   * <p>This is valid per nbformat spec but might fail validation.
   */
  @Test
  void testEmptyNotebook_MinimalMetadata() throws Exception {
    String emptyNotebook =
        """
        {
          "cells": [],
          "metadata": {},
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    System.out.println("\n=== Test Case 1: Empty Notebook with Minimal Metadata ===");
    System.out.println("JSON: " + emptyNotebook);

    // First test: Does the validator accept it?
    boolean isValid = validator.validateNotebook(emptyNotebook);
    System.out.println("Validator result: " + (isValid ? "✅ VALID" : "❌ INVALID"));
    assertTrue(isValid, "Empty notebook should be valid per nbformat spec");

    // Second test: Can we parse it to DTO?
    JupyterNotebookDTO notebookDto;
    try {
      notebookDto = objectMapper.readValue(emptyNotebook, JupyterNotebookDTO.class);
      System.out.println("DTO parsing: ✅ SUCCESS");
    } catch (Exception e) {
      System.out.println("DTO parsing: ❌ FAILED - " + e.getMessage());
      fail("Should be able to parse empty notebook to DTO");
      return;
    }

    // Third test: Create request and try to save
    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebookDto);

    try {
      JupyterNotebookSaved result =
          notebookService.uploadNotebook(request, sessionId, "test.example.com");
      System.out.println("Service upload: ✅ SUCCESS");
      System.out.println("Notebook saved with ID: " + result.getId());
      System.out.println("Readable ID: " + result.getReadableId());

      // Verify it was actually saved
      assertTrue(repository.existsById(result.getId()));
      System.out.println("\n✅ ISSUE APPEARS TO BE FIXED - Empty notebook accepted");

    } catch (InvalidNotebookException e) {
      System.out.println("Service upload: ❌ VALIDATION ERROR");
      System.out.println("Error message: " + e.getMessage());
      System.out.println("\n❌ BUG CONFIRMED - Empty notebook rejected by validation");
      fail("Empty notebook should be valid but was rejected: " + e.getMessage());

    } catch (DataIntegrityViolationException e) {
      System.out.println("Service upload: ❌ DATABASE ERROR");
      System.out.println("Error: " + e.getMessage());
      System.out.println("\n❌ BUG CONFIRMED - Database constraints prevent saving");
      fail("Database constraints violated: " + e.getMessage());

    } catch (Exception e) {
      System.out.println("Service upload: ❌ UNEXPECTED ERROR");
      System.out.println("Error type: " + e.getClass().getName());
      System.out.println("Error message: " + e.getMessage());
      fail("Unexpected error: " + e.getMessage());
    }
  }

  /** Test Case 2: Markdown-only notebook */
  @Test
  void testMarkdownOnlyNotebook() throws Exception {
    String markdownNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "markdown",
              "id": "cell-1",
              "metadata": {},
              "source": ["# Documentation\\n", "This notebook contains only text."]
            }
          ],
          "metadata": {
            "title": "README"
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    System.out.println("\n=== Test Case 2: Markdown-only Notebook ===");

    // Validate
    boolean isValid = validator.validateNotebook(markdownNotebook);
    System.out.println("Validator: " + (isValid ? "✅ VALID" : "❌ INVALID"));

    // Parse to DTO
    JupyterNotebookDTO notebookDto =
        objectMapper.readValue(markdownNotebook, JupyterNotebookDTO.class);

    // Try to save
    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebookDto);

    try {
      JupyterNotebookSaved result =
          notebookService.uploadNotebook(request, sessionId, "test.example.com");
      System.out.println("✅ Markdown-only notebook accepted - ID: " + result.getId());
    } catch (Exception e) {
      System.out.println("❌ Markdown-only notebook rejected: " + e.getMessage());
      fail("Markdown-only notebook should be valid: " + e.getMessage());
    }
  }

  /** Test Case 3: Partial metadata (only language name) */
  @Test
  void testPartialMetadata_OnlyLanguageName() throws Exception {
    String partialMetadata =
        """
        {
          "cells": [],
          "metadata": {
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    System.out.println("\n=== Test Case 3: Partial Metadata (language name only) ===");

    // Validate
    boolean isValid = validator.validateNotebook(partialMetadata);
    System.out.println("Validator: " + (isValid ? "✅ VALID" : "❌ INVALID"));

    // Parse to DTO
    JupyterNotebookDTO notebookDto =
        objectMapper.readValue(partialMetadata, JupyterNotebookDTO.class);

    // Try to save
    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebookDto);

    try {
      JupyterNotebookSaved result =
          notebookService.uploadNotebook(request, sessionId, "test.example.com");
      System.out.println("✅ Partial metadata notebook accepted - ID: " + result.getId());
    } catch (DataIntegrityViolationException e) {
      System.out.println("❌ DATABASE ERROR: " + e.getMessage());
      if (e.getMessage().contains("null value") || e.getMessage().contains("constraint")) {
        System.out.println("This confirms database constraints on nullable fields");
      }
      fail("Database constraint violation: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("❌ ERROR: " + e.getClass().getName() + " - " + e.getMessage());
      fail("Partial metadata should be valid: " + e.getMessage());
    }
  }

  /** Test Case 4: No metadata fields at all (testing defaults) */
  @Test
  void testCompletelyEmptyMetadata() throws Exception {
    String minimalNotebook =
        """
        {
          "cells": [],
          "metadata": {},
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    System.out.println("\n=== Test Case 4: Completely Empty Metadata ===");

    JupyterNotebookDTO notebookDto =
        objectMapper.readValue(minimalNotebook, JupyterNotebookDTO.class);

    // Check what values the DTO has
    System.out.println("DTO metadata: " + notebookDto.getMetadata());
    if (notebookDto.getMetadata() != null) {
      System.out.println("  kernelspec: " + notebookDto.getMetadata().getKernelspec());
      System.out.println("  language_info: " + notebookDto.getMetadata().getLanguageInfo());
    }

    JupyterNotebookRequest request = new JupyterNotebookRequest();
    request.setNotebook(notebookDto);

    try {
      JupyterNotebookSaved result =
          notebookService.uploadNotebook(request, sessionId, "test.example.com");
      System.out.println("✅ Empty metadata accepted - Issue is FIXED");
      System.out.println("Saved with ID: " + result.getId());

      // Check what was actually saved in the database
      var savedEntity = repository.findById(result.getId()).orElse(null);
      if (savedEntity != null) {
        System.out.println("\nDatabase values:");
        System.out.println("  kernelName: " + savedEntity.getKernelName());
        System.out.println("  kernelDisplayName: " + savedEntity.getKernelDisplayName());
        System.out.println("  language: " + savedEntity.getLanguage());
        System.out.println("  fileExtension: " + savedEntity.getFileExtension());
        System.out.println("  languageVersion: " + savedEntity.getLanguageVersion());
      }
    } catch (Exception e) {
      System.out.println("❌ Empty metadata rejected");
      System.out.println("Error: " + e.getClass().getName());
      System.out.println("Message: " + e.getMessage());
      e.printStackTrace();
      fail("Empty metadata should be valid: " + e.getMessage());
    }
  }
}
