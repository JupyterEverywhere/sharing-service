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
    assertTrue(result, "Absolute minimum valid notebook per nbformat v4.5 spec should pass validation");
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
}
