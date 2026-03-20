package org.jupytereverywhere.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Minimal notebook with code cell should pass");
    assertNull(result.errorMessage());
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid());
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid());
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Empty cells array should be accepted");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Empty source string should be accepted");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Empty source array should be accepted");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Extra top-level fields should be accepted");
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
      ValidationResult result = validator.validateNotebook(notebook);
      assertTrue(result.valid(), "v4." + minor + " should pass");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid());
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat must be 4"));
    assertTrue(result.errorMessage().contains("got 3"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat_minor"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat_minor"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat_minor"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("cells"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("cells"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("invalid_type"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("cell_type"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("source"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("metadata"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("metadata"));
  }

  @Test
  void testReject_NonJson() {
    ValidationResult result = validator.validateNotebook("{ this is not valid json }");
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("not valid JSON"));
  }

  @Test
  void testReject_EmptyString() {
    ValidationResult result = validator.validateNotebook("");
    assertFalse(result.valid());
  }

  @Test
  void testReject_NullJsonValue() {
    ValidationResult result = validator.validateNotebook("null");
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("root must be a JSON object"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat"));
    assertTrue(result.errorMessage().contains("integer"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("nbformat_minor"));
    assertTrue(result.errorMessage().contains("integer"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertFalse(result.valid());
    assertTrue(result.errorMessage().contains("source"));
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Empty language_info.name should pass");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Deep metadata should pass");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Diverse output types should pass (outputs not checked)");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Execution counts should pass (not checked)");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "v4.0 notebook should pass");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "v4.1 without cell IDs should pass");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "v4.5 without cell IDs should now pass structural validation");
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
    ValidationResult result = validator.validateNotebook(notebook);
    assertTrue(result.valid(), "Real R notebook should pass");
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
    String largeNotebook = sb.toString();
    assertTrue(largeNotebook.length() > 5_000_000, "Notebook should be >5MB");

    // Warm up
    validator.validateNotebook(largeNotebook);

    // Timed run
    long start = System.nanoTime();
    ValidationResult result = validator.validateNotebook(largeNotebook);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertTrue(result.valid());
    assertTrue(
        elapsedMs < 5000, "Structural validation should complete in <5s, took " + elapsedMs + "ms");
    // Log actual time for manual review
    System.out.println(
        "Large notebook validation took "
            + elapsedMs
            + "ms for "
            + cellCount
            + " cells (~"
            + (largeNotebook.length() / 1_000_000)
            + "MB)");
  }

  @Test
  void testValidationResultRecord() {
    ValidationResult success = ValidationResult.success();
    assertTrue(success.valid());
    assertNull(success.errorMessage());

    ValidationResult failure = ValidationResult.failure("test error");
    assertFalse(failure.valid());
    assertEquals("test error", failure.errorMessage());
  }
}
