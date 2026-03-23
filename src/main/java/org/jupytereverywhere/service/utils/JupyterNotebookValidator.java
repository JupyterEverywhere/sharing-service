package org.jupytereverywhere.service.utils;

import java.util.Set;

import org.apache.logging.log4j.message.StringMapMessage;
import org.jupytereverywhere.exception.InvalidNotebookException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class JupyterNotebookValidator {

  private static final String MESSAGE = "Message";
  private static final Set<String> VALID_CELL_TYPES = Set.of("code", "markdown", "raw");
  private static final int REQUIRED_NBFORMAT = 4;
  private static final int MIN_MINOR_VERSION = 0;
  private static final int MAX_MINOR_VERSION = 5;

  private final ObjectMapper objectMapper;

  public JupyterNotebookValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    log.info(
        new StringMapMessage()
            .with(MESSAGE, "JupyterNotebookValidator initialized (structural checks)"));
  }

  public JsonNode validateNotebook(byte[] notebookBytes) {
    JsonNode root;
    try {
      root = objectMapper.readTree(notebookBytes);
    } catch (Exception e) {
      log.warn(
          new StringMapMessage()
              .with(MESSAGE, "Notebook validation failed: invalid JSON")
              .with("Error", e.getMessage()));
      throw new InvalidNotebookException("Invalid notebook: content is not valid JSON");
    }

    if (root == null || !root.isObject()) {
      throw new InvalidNotebookException("Invalid notebook: root must be a JSON object");
    }

    // nbformat must exist and equal 4
    JsonNode nbformatNode = root.get("nbformat");
    if (nbformatNode == null || !nbformatNode.isInt()) {
      throw new InvalidNotebookException(
          "Invalid notebook: nbformat field is required and must be an integer");
    }
    if (nbformatNode.intValue() != REQUIRED_NBFORMAT) {
      throw new InvalidNotebookException(
          "Invalid notebook: nbformat must be "
              + REQUIRED_NBFORMAT
              + ", got "
              + nbformatNode.intValue());
    }

    // nbformat_minor must exist and be in range 0-5
    JsonNode minorNode = root.get("nbformat_minor");
    if (minorNode == null || !minorNode.isInt()) {
      throw new InvalidNotebookException(
          "Invalid notebook: nbformat_minor field is required and must be an integer");
    }
    int minor = minorNode.intValue();
    if (minor < MIN_MINOR_VERSION || minor > MAX_MINOR_VERSION) {
      throw new InvalidNotebookException(
          "Invalid notebook: nbformat_minor must be between "
              + MIN_MINOR_VERSION
              + " and "
              + MAX_MINOR_VERSION
              + ", got "
              + minor);
    }

    // metadata must exist and be an object
    JsonNode metadataNode = root.get("metadata");
    if (metadataNode == null || !metadataNode.isObject()) {
      throw new InvalidNotebookException(
          "Invalid notebook: metadata field is required and must be an object");
    }

    // cells must exist and be an array
    JsonNode cellsNode = root.get("cells");
    if (cellsNode == null || !cellsNode.isArray()) {
      throw new InvalidNotebookException(
          "Invalid notebook: cells field is required and must be an array");
    }

    // Validate each cell
    for (int i = 0; i < cellsNode.size(); i++) {
      JsonNode cell = cellsNode.get(i);

      JsonNode cellTypeNode = cell.get("cell_type");
      if (cellTypeNode == null || !cellTypeNode.isTextual()) {
        throw new InvalidNotebookException(
            "Invalid notebook: cells[" + i + "] is missing cell_type or cell_type is not a string");
      }
      if (!VALID_CELL_TYPES.contains(cellTypeNode.textValue())) {
        throw new InvalidNotebookException(
            "Invalid notebook: cells["
                + i
                + "] has invalid cell_type '"
                + cellTypeNode.textValue()
                + "', must be one of: code, markdown, raw");
      }

      JsonNode sourceNode = cell.get("source");
      if (sourceNode == null) {
        throw new InvalidNotebookException(
            "Invalid notebook: cells[" + i + "] is missing source field");
      }
      if (!sourceNode.isTextual() && !sourceNode.isArray()) {
        throw new InvalidNotebookException(
            "Invalid notebook: cells[" + i + "] source must be a string or array");
      }
    }

    log.debug(
        new StringMapMessage()
            .with(MESSAGE, "Notebook validation passed")
            .with("NbformatMinor", String.valueOf(minor))
            .with("CellCount", String.valueOf(cellsNode.size())));

    return root;
  }
}
