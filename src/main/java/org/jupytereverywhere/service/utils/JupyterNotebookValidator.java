package org.jupytereverywhere.service.utils;

import java.io.InputStream;
import java.util.Set;

import org.apache.logging.log4j.message.StringMapMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import lombok.extern.log4j.Log4j2;

/**
 * Native Java validator for Jupyter Notebook JSON using the nbformat JSON Schema. This replaces the
 * Python subprocess-based validation for improved performance.
 */
@Log4j2
@Component
@Primary
public class JupyterNotebookValidator {

  public static final String MESSAGE = "Message";
  private static final String SCHEMA_PATH = "/schemas/nbformat.v4.schema.json";

  private final JsonSchema schema;
  private final ObjectMapper objectMapper;

  public JupyterNotebookValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.schema = loadSchema();

    log.info(
        new StringMapMessage()
            .with(MESSAGE, "JupyterNotebookValidator initialized")
            .with("SchemaPath", SCHEMA_PATH));
  }

  private JsonSchema loadSchema() {
    try {
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
      InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_PATH);

      if (schemaStream == null) {
        throw new IllegalStateException("Could not load JSON schema from " + SCHEMA_PATH);
      }

      return factory.getSchema(schemaStream);
    } catch (Exception e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE, "Failed to load JSON schema")
              .with("SchemaPath", SCHEMA_PATH)
              .with("ExceptionType", e.getClass().getSimpleName())
              .with("ExceptionMessage", e.getMessage()),
          e);
      throw new IllegalStateException("Failed to initialize notebook validator", e);
    }
  }

  /**
   * Validates a Jupyter Notebook JSON string against the nbformat v4 schema.
   *
   * @param notebookJson The notebook JSON string to validate
   * @return true if the notebook is valid, false otherwise
   */
  public boolean validateNotebook(String notebookJson) {
    try {
      JsonNode jsonNode = objectMapper.readTree(notebookJson);
      Set<ValidationMessage> errors = schema.validate(jsonNode);

      if (errors.isEmpty()) {
        log.debug(new StringMapMessage().with(MESSAGE, "Notebook validation passed"));
        return true;
      } else {
        log.warn(
            new StringMapMessage()
                .with(MESSAGE, "Notebook validation failed")
                .with("ErrorCount", String.valueOf(errors.size()))
                .with("Errors", errors.toString()));
        return false;
      }
    } catch (Exception e) {
      log.error(
          new StringMapMessage()
              .with(MESSAGE, "Exception during notebook validation")
              .with("ExceptionType", e.getClass().getSimpleName())
              .with("ExceptionMessage", e.getMessage()),
          e);
      return false;
    }
  }
}
