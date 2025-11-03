package org.jupytereverywhere.service.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class JupyterNotebookValidatorTest {

  private JupyterNotebookValidator validator;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    validator = new JupyterNotebookValidator(objectMapper);
  }

  @Test
  void testValidatorInitialization() {
    assertNotNull(validator);
  }

  @Test
  void testValidateNotebook_ValidMinimalNotebook() {
    String validNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "code",
              "execution_count": null,
              "id": "cell-1",
              "metadata": {},
              "outputs": [],
              "source": []
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validNotebook);
    assertTrue(result, "Minimal valid notebook should pass validation");
  }

  @Test
  void testValidateNotebook_ValidNotebookWithContent() {
    String validNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "markdown",
              "id": "cell-1",
              "metadata": {},
              "source": ["# Example Notebook"]
            },
            {
              "cell_type": "code",
              "execution_count": 1,
              "id": "cell-2",
              "metadata": {},
              "outputs": [
                {
                  "output_type": "stream",
                  "name": "stdout",
                  "text": ["Hello, World!\\n"]
                }
              ],
              "source": ["print('Hello, World!')"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python",
              "version": "3.9.0"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validNotebook);
    assertTrue(result, "Valid notebook with content should pass validation");
  }

  @Test
  void testValidateNotebook_ValidEmptyNotebook() {
    String validEmptyNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validEmptyNotebook);
    assertTrue(result, "Empty notebook with all required fields should pass validation");
  }

  @Test
  void testValidateNotebook_ValidEmptyNotebookMinimalMetadata() {
    String validEmptyNotebook =
        """
        {
          "cells": [],
          "metadata": {},
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validEmptyNotebook);
    assertTrue(
        result, "Absolute minimum valid notebook per nbformat v4.5 spec should pass validation");
  }

  @Test
  void testValidateNotebook_ValidEmptyNotebookWithKernelspecOnly() {
    String validEmptyNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3 (ipykernel)",
              "name": "python3"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validEmptyNotebook);
    assertTrue(result, "Empty notebook with only kernelspec should pass validation");
  }

  @Test
  void testValidateNotebook_ValidEmptyNotebookWithLanguageInfoOnly() {
    String validEmptyNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "language_info": {
              "name": "python",
              "version": "3.9.0",
              "mimetype": "text/x-python",
              "file_extension": ".py"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validEmptyNotebook);
    assertTrue(result, "Empty notebook with only language_info should pass validation");
  }

  @Test
  void testValidateNotebook_ValidNotebookWithEmptyLanguageInfoName() {
    String validNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "language_info": {
              "name": ""
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validNotebook);
    assertTrue(
        result,
        "Notebook with empty language_info.name should pass validation per nbformat 4.5 spec");
  }

  @Test
  void testValidateNotebook_ValidEmptyNotebookHigherMinorVersion() {
    String validEmptyNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 6
        }
        """;

    boolean result = validator.validateNotebook(validEmptyNotebook);
    assertTrue(result, "Empty notebook with higher nbformat_minor should pass validation");
  }

  @Test
  void testValidateNotebook_MissingRequiredField_cells() {
    String invalidNotebook =
        """
        {
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Notebook missing 'cells' field should fail validation");
  }

  @Test
  void testValidateNotebook_MissingRequiredField_metadata() {
    String invalidNotebook =
        """
        {
          "cells": [],
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Notebook missing 'metadata' field should fail validation");
  }

  @Test
  void testValidateNotebook_MissingRequiredField_nbformat() {
    String invalidNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Notebook missing 'nbformat' field should fail validation");
  }

  @Test
  void testValidateNotebook_MissingRequiredField_nbformat_minor() {
    String invalidNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Notebook missing 'nbformat_minor' field should fail validation");
  }

  @Test
  void testValidateNotebook_InvalidCellType() {
    String invalidNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "invalid_type",
              "metadata": {},
              "source": []
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Notebook with invalid cell type should fail validation");
  }

  @Test
  void testValidateNotebook_WrongNbformatVersion() {
    String invalidNotebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 3,
          "nbformat_minor": 0
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Notebook with wrong nbformat version should fail validation");
  }

  @Test
  void testValidateNotebook_InvalidJSON() {
    String invalidJson = "{ this is not valid json }";

    boolean result = validator.validateNotebook(invalidJson);
    assertFalse(result, "Invalid JSON should fail validation");
  }

  @Test
  void testValidateNotebook_EmptyString() {
    String emptyString = "";

    boolean result = validator.validateNotebook(emptyString);
    assertFalse(result, "Empty string should fail validation");
  }

  @Test
  void testValidateNotebook_NullValue() {
    String nullString = "null";

    boolean result = validator.validateNotebook(nullString);
    assertFalse(result, "Null value should fail validation");
  }

  @Test
  void testValidateNotebook_ValidRawCell() {
    String validNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "raw",
              "id": "cell-1",
              "metadata": {},
              "source": ["Raw text content"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validNotebook);
    assertTrue(result, "Valid notebook with raw cell should pass validation");
  }

  @Test
  void testValidateNotebook_CodeCellMissingRequiredFields() {
    String invalidNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "code",
              "source": ["print('test')"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(invalidNotebook);
    assertFalse(result, "Code cell missing required fields should fail validation");
  }

  @Test
  void testValidateNotebook_ValidMultipleCells() {
    String validNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "markdown",
              "id": "cell-1",
              "metadata": {},
              "source": ["# Title"]
            },
            {
              "cell_type": "code",
              "execution_count": null,
              "id": "cell-2",
              "metadata": {},
              "outputs": [],
              "source": ["import numpy as np"]
            },
            {
              "cell_type": "code",
              "execution_count": null,
              "id": "cell-3",
              "metadata": {},
              "outputs": [],
              "source": ["x = np.array([1, 2, 3])"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(validNotebook);
    assertTrue(result, "Valid notebook with multiple cells should pass validation");
  }

  @Test
  void testValidateNotebook_ValidV41NotebookWithoutCellIds() {
    String validV41Notebook =
        """
        {
          "cells": [
            {
              "cell_type": "code",
              "execution_count": null,
              "metadata": {},
              "outputs": [],
              "source": ["print('Hello from v4.1')"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 1
        }
        """;

    boolean result = validator.validateNotebook(validV41Notebook);
    assertTrue(result, "Valid v4.1 notebook without cell IDs should pass validation");
  }

  @Test
  void testValidateNotebook_ValidV40Notebook() {
    String validV40Notebook =
        """
        {
          "cells": [
            {
              "cell_type": "markdown",
              "metadata": {},
              "source": ["# Title"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "R",
              "name": "ir"
            },
            "language_info": {
              "name": "R"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 0
        }
        """;

    boolean result = validator.validateNotebook(validV40Notebook);
    assertTrue(result, "Valid v4.0 notebook should pass validation");
  }

  @Test
  void testValidateNotebook_RealRNotebookSample() {
    // Simplified version of the real R notebook structure
    String rNotebook =
        """
        {
          "cells": [
            {
              "cell_type": "code",
              "execution_count": null,
              "metadata": {},
              "outputs": [],
              "source": ["library(coursekata)"]
            }
          ],
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
          "nbformat": 4,
          "nbformat_minor": 1
        }
        """;

    boolean result = validator.validateNotebook(rNotebook);
    assertTrue(result, "Real-world R notebook structure (v4.1) should pass validation");
  }

  @Test
  void testValidateNotebook_NegativeMinorVersion() {
    String notebook =
        """
        {
          "cells": [],
          "metadata": {},
          "nbformat": 4,
          "nbformat_minor": -1
        }
        """;

    boolean result = validator.validateNotebook(notebook);
    assertFalse(result, "Notebook with negative nbformat_minor should fail validation");
  }

  @Test
  void testValidateNotebook_VeryLargeMinorVersion() {
    String notebook =
        """
        {
          "cells": [],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 100
        }
        """;

    boolean result = validator.validateNotebook(notebook);
    // Should use v4.5 schema and pass if it meets v4.5 requirements
    assertTrue(result, "Notebook with very large nbformat_minor should fall back to v4.5");
  }

  @Test
  void testValidateNotebook_MinorVersionAsString() {
    String notebook =
        """
        {
          "cells": [],
          "metadata": {},
          "nbformat": 4,
          "nbformat_minor": "5"
        }
        """;

    boolean result = validator.validateNotebook(notebook);
    assertFalse(result, "Notebook with nbformat_minor as string should fail validation");
  }

  @Test
  void testValidateNotebook_AllIntermediateVersions() {
    // Test v4.2, v4.3, v4.4 explicitly
    for (int minor = 2; minor <= 4; minor++) {
      String notebook =
          String.format(
              """
              {
                "cells": [],
                "metadata": {
                  "kernelspec": {
                    "display_name": "Python 3",
                    "name": "python3"
                  },
                  "language_info": {
                    "name": "python"
                  }
                },
                "nbformat": 4,
                "nbformat_minor": %d
              }
              """,
              minor);

      boolean result = validator.validateNotebook(notebook);
      assertTrue(result, "v4." + minor + " notebook should pass validation");
    }
  }

  @Test
  void testValidateNotebook_V45WithoutCellIds_ShouldFail() {
    // v4.5 REQUIRES cell IDs, so this should fail when using v4.5 schema
    String v45NotebookWithoutIds =
        """
        {
          "cells": [
            {
              "cell_type": "code",
              "execution_count": null,
              "metadata": {},
              "outputs": [],
              "source": ["print('test')"]
            }
          ],
          "metadata": {
            "kernelspec": {
              "display_name": "Python 3",
              "name": "python3"
            },
            "language_info": {
              "name": "python"
            }
          },
          "nbformat": 4,
          "nbformat_minor": 5
        }
        """;

    boolean result = validator.validateNotebook(v45NotebookWithoutIds);
    assertFalse(result, "v4.5 notebook without cell IDs should fail validation");
  }
}
