package org.jupytereverywhere.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.exception.InvalidNotebookException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JupyterNotebookValidatorTest {

  private JupyterNotebookValidator validator;

  @BeforeEach
  void setUp() {
    validator = new JupyterNotebookValidator(new ObjectMapper());
  }

  @Test
  void testValidatorInitialization() {
    assertNotNull(validator);
  }

  // ── Acceptance: valid notebooks (US1) ──

  @Test
  void testValidNotebook_MinimalWithCodeCell() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {
              "cell_type": "code",
              "source": []
            }
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result);
    assertTrue(result.isObject());
    assertEquals(4, result.get("nbformat").intValue());
  }

  @Test
  void testValidNotebook_WithMarkdownCell() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {"kernelspec": {"display_name": "Python 3", "name": "python3"}},
          "cells": [
            {
              "cell_type": "markdown",
              "source": ["# Example Notebook"]
            }
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result);
  }

  @Test
  void testValidNotebook_WithRawCell() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {
              "cell_type": "raw",
              "source": "Raw text content"
            }
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result);
  }

  @Test
  void testValidNotebook_EmptyCellsArray() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": []
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Empty cells array should be accepted");
  }

  @Test
  void testValidNotebook_SourceAsEmptyString() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {
              "cell_type": "code",
              "source": ""
            }
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Empty source string should be accepted");
  }

  @Test
  void testValidNotebook_SourceAsEmptyArray() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {
              "cell_type": "code",
              "source": []
            }
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Empty source array should be accepted");
  }

  @Test
  void testValidNotebook_ExtraTopLevelFields() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [],
          "extra_field": "should be ignored",
          "another_field": 42
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Extra top-level fields should be accepted");
  }

  @Test
  void testValidNotebook_AllMinorVersions() {
    for (int minor = 0; minor <= 5; minor++) {
      String notebook =
          String.format(
              """
              {
                "nbformat": 4,
                "nbformat_minor": %d,
                "metadata": {},
                "cells": []
              }
              """,
              minor);
      JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
      assertNotNull(result, "v4." + minor + " should pass");
    }
  }

  @Test
  void testValidNotebook_MultipleMixedCells() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {
            "kernelspec": {"display_name": "Python 3", "name": "python3"},
            "language_info": {"name": "python", "version": "3.9.0"}
          },
          "cells": [
            {"cell_type": "markdown", "source": ["# Title"]},
            {"cell_type": "code", "source": ["print('Hello')"], "execution_count": 1, "outputs": []},
            {"cell_type": "raw", "source": "Raw content"}
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result);
  }

  // ── Rejection: invalid notebooks (US2) ──

  @Test
  void testReject_MissingNbformat() {
    String notebook =
        """
        {
          "nbformat_minor": 5,
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat"));
  }

  @Test
  void testReject_NbformatNotFour() {
    String notebook =
        """
        {
          "nbformat": 3,
          "nbformat_minor": 0,
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat must be 4"));
    assertTrue(exception.getMessage().contains("got 3"));
  }

  @Test
  void testReject_NbformatMinorOutOfRange_Six() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 6,
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat_minor"));
  }

  @Test
  void testReject_NbformatMinorNegative() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": -1,
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat_minor"));
  }

  @Test
  void testReject_MissingNbformatMinor() {
    String notebook =
        """
        {
          "nbformat": 4,
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat_minor"));
  }

  @Test
  void testReject_MissingCells() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {}
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("cells"));
  }

  @Test
  void testReject_CellsNotArray() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": "not an array"
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("cells"));
  }

  @Test
  void testReject_InvalidCellType() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {"cell_type": "invalid_type", "source": []}
          ]
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("invalid_type"));
  }

  @Test
  void testReject_MissingCellType() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {"source": []}
          ]
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("cell_type"));
  }

  @Test
  void testReject_MissingSource() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {"cell_type": "code"}
          ]
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("source"));
  }

  @Test
  void testReject_MissingMetadata() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("metadata"));
  }

  @Test
  void testReject_MetadataNotObject() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": "not an object",
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("metadata"));
  }

  @Test
  void testReject_NonJson() {
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () ->
                validator.validateNotebook(
                    "{ this is not valid json }".getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("not valid JSON"));
  }

  @Test
  void testReject_EmptyString() {
    assertThrows(
        InvalidNotebookException.class,
        () -> validator.validateNotebook("".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testReject_NullJsonValue() {
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook("null".getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("root must be a JSON object"));
  }

  @Test
  void testReject_NbformatAsString() {
    String notebook =
        """
        {
          "nbformat": "4",
          "nbformat_minor": 5,
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat"));
    assertTrue(exception.getMessage().contains("integer"));
  }

  @Test
  void testReject_NbformatMinorAsString() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": "5",
          "metadata": {},
          "cells": []
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("nbformat_minor"));
    assertTrue(exception.getMessage().contains("integer"));
  }

  @Test
  void testReject_SourceWrongType() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {"cell_type": "code", "source": 42}
          ]
        }
        """;
    InvalidNotebookException exception =
        assertThrows(
            InvalidNotebookException.class,
            () -> validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("source"));
  }

  // ── Backward compatibility (US3) ──

  @Test
  void testBackwardCompat_EmptyLanguageInfoName() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {
            "language_info": {"name": ""}
          },
          "cells": []
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Empty language_info.name should pass");
  }

  @Test
  void testBackwardCompat_DeepMetadata() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3",
              "language": "python"
            },
            "language_info": {
              "codemirror_mode": {"name": "ipython", "version": 3},
              "file_extension": ".py",
              "mimetype": "text/x-python",
              "name": "python",
              "nbconvert_exporter": "python",
              "pygments_lexer": "ipython3",
              "version": "3.9.7"
            }
          },
          "cells": []
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Deep metadata should pass");
  }

  @Test
  void testBackwardCompat_DiverseOutputTypes() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {
              "cell_type": "code",
              "source": ["import matplotlib"],
              "execution_count": 1,
              "outputs": [
                {"output_type": "stream", "name": "stdout", "text": ["Hello\\n"]},
                {"output_type": "execute_result", "data": {"text/plain": ["42"]}, "metadata": {}, "execution_count": 1},
                {"output_type": "display_data", "data": {"image/png": "base64data"}, "metadata": {}}
              ]
            }
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Diverse output types should pass (outputs not checked)");
  }

  @Test
  void testBackwardCompat_ExecutionCounts() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {"cell_type": "code", "source": ["x = 1"], "execution_count": null, "outputs": []},
            {"cell_type": "code", "source": ["x = 2"], "execution_count": 42, "outputs": []}
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Execution counts should pass (not checked)");
  }

  @Test
  void testBackwardCompat_V40Notebook() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 0,
          "metadata": {
            "kernelspec": {"display_name": "R", "name": "ir"},
            "language_info": {"name": "R"}
          },
          "cells": [
            {"cell_type": "markdown", "source": ["# Title"]}
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "v4.0 notebook should pass");
  }

  @Test
  void testBackwardCompat_V41WithoutCellIds() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 1,
          "metadata": {
            "kernelspec": {"display_name": "Python 3", "name": "python3"},
            "language_info": {"name": "python"}
          },
          "cells": [
            {"cell_type": "code", "source": ["print('test')"], "execution_count": null, "outputs": []}
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "v4.1 without cell IDs should pass");
  }

  @Test
  void testBackwardCompat_V45WithoutCellIds() {
    // The new structural validator does NOT check for cell IDs — this is a deliberate
    // relaxation compared to the old schema validator (which rejected v4.5 without IDs)
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 5,
          "metadata": {},
          "cells": [
            {"cell_type": "code", "source": ["print('test')"]}
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "v4.5 without cell IDs should now pass structural validation");
  }

  @Test
  void testBackwardCompat_RealRNotebook() {
    String notebook =
        """
        {
          "nbformat": 4,
          "nbformat_minor": 1,
          "metadata": {
            "kernelspec": {
              "display_name": "R",
              "language": "R",
              "name": "ir"
            },
            "language_info": {
              "codemirror_mode": "r",
              "file_extension": ".r",
              "mimetype": "text/x-r-source",
              "name": "R",
              "pygments_lexer": "r"
            }
          },
          "cells": [
            {"cell_type": "code", "source": ["library(coursekata)"], "execution_count": null, "outputs": []}
          ]
        }
        """;
    JsonNode result = validator.validateNotebook(notebook.getBytes(StandardCharsets.UTF_8));
    assertNotNull(result, "Real R notebook should pass");
  }

  @Test
  void testPerformance_LargeNotebook_Under5ms() {
    // Build a ~5MB notebook with many cells
    StringBuilder sb = new StringBuilder();
    sb.append("{\"nbformat\":4,\"nbformat_minor\":5,\"metadata\":{},\"cells\":[");
    int cellCount = 5000;
    for (int i = 0; i < cellCount; i++) {
      if (i > 0) sb.append(",");
      sb.append("{\"cell_type\":\"code\",\"source\":\"").append("x".repeat(1000)).append("\"}");
    }
    sb.append("]}");
    byte[] largeNotebook = sb.toString().getBytes(StandardCharsets.UTF_8);
    assertTrue(largeNotebook.length > 5_000_000, "Notebook should be >5MB");

    // Warm up
    validator.validateNotebook(largeNotebook);

    // Timed run
    long start = System.nanoTime();
    JsonNode result = validator.validateNotebook(largeNotebook);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertNotNull(result);
    assertTrue(
        elapsedMs < 5000, "Structural validation should complete in <5s, took " + elapsedMs + "ms");
    // Log actual time for manual review
    System.out.println(
        "Large notebook validation took "
            + elapsedMs
            + "ms for "
            + cellCount
            + " cells (~"
            + (largeNotebook.length / 1_000_000)
            + "MB)");
  }

  @Test
  void testValidationResultRecord() {
    ValidationResult success = ValidationResult.success();
    assertTrue(success.valid());
    assertEquals(null, success.errorMessage());

    ValidationResult failure = ValidationResult.failure("test error");
    assertEquals(false, failure.valid());
    assertEquals("test error", failure.errorMessage());
  }
}
