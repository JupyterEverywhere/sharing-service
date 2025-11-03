package org.jupytereverywhere.service.utils;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
  private static final String SCHEMA_DIR = "/schemas/";
  private static final String SCHEMA_TEMPLATE = "nbformat.v4.%d.schema.json";
  private static final int DEFAULT_MINOR_VERSION = 5;
  private static final int MIN_MINOR_VERSION = 0;
  private static final int MAX_MINOR_VERSION = 5;

  private final Map<Integer, JsonSchema> schemasByMinorVersion;
  private final ObjectMapper objectMapper;

  public JupyterNotebookValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.schemasByMinorVersion = loadAllSchemas();

    log.info(
        new StringMapMessage()
            .with(MESSAGE, "JupyterNotebookValidator initialized")
            .with("SchemasLoaded", String.valueOf(schemasByMinorVersion.size()))
            .with("SupportedVersions", "4.0 through 4.5"));
  }

  /**
   * Loads all nbformat v4 schemas (versions 4.0 through 4.5) from classpath resources.
   *
   * <p>If a specific version's schema cannot be loaded, that version will fall back to the default
   * v4.5 schema during validation. Only the default v4.5 schema is required - the application will
   * fail to start if it cannot be loaded.
   *
   * @return An immutable map of minor version numbers to their corresponding JsonSchema objects
   * @throws IllegalStateException if no schemas can be loaded or if the default v4.5 schema is
   *     missing
   */
  private Map<Integer, JsonSchema> loadAllSchemas() {
    Map<Integer, JsonSchema> schemas = new HashMap<>();
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

    for (int minorVersion = MIN_MINOR_VERSION; minorVersion <= MAX_MINOR_VERSION; minorVersion++) {
      String schemaPath = SCHEMA_DIR + String.format(SCHEMA_TEMPLATE, minorVersion);
      InputStream schemaStream = getClass().getResourceAsStream(schemaPath);

      if (schemaStream == null) {
        log.warn(
            new StringMapMessage()
                .with(MESSAGE, "Could not load schema for minor version")
                .with("MinorVersion", String.valueOf(minorVersion))
                .with("SchemaPath", schemaPath));
        continue;
      }

      try (schemaStream) {
        JsonSchema schema = factory.getSchema(schemaStream);
        schemas.put(minorVersion, schema);

        log.debug(
            new StringMapMessage()
                .with(MESSAGE, "Loaded schema for nbformat version")
                .with("MinorVersion", String.valueOf(minorVersion))
                .with("SchemaPath", schemaPath));

      } catch (Exception e) {
        log.error(
            new StringMapMessage()
                .with(MESSAGE, "Failed to load schema for minor version")
                .with("MinorVersion", String.valueOf(minorVersion))
                .with("ExceptionType", e.getClass().getSimpleName())
                .with("ExceptionMessage", e.getMessage()),
            e);
      }
    }

    if (schemas.isEmpty()) {
      throw new IllegalStateException(
          "Failed to load any nbformat schemas. Validator cannot function.");
    }

    if (!schemas.containsKey(DEFAULT_MINOR_VERSION)) {
      throw new IllegalStateException(
          "Failed to load default schema (v4."
              + DEFAULT_MINOR_VERSION
              + "). Validator cannot function.");
    }

    return Collections.unmodifiableMap(schemas);
  }

  /**
   * Extracts the nbformat_minor field from the notebook JSON.
   *
   * @param jsonNode The parsed notebook JSON
   * @return The nbformat_minor value, or null if not present or invalid
   */
  private Integer extractNbformatMinor(JsonNode jsonNode) {
    try {
      JsonNode minorNode = jsonNode.get("nbformat_minor");
      if (minorNode != null && minorNode.isInt()) {
        return minorNode.asInt();
      }
      return null;
    } catch (Exception e) {
      log.debug(
          new StringMapMessage()
              .with(MESSAGE, "Could not extract nbformat_minor")
              .with("ExceptionMessage", e.getMessage()));
      return null;
    }
  }

  /**
   * Selects the appropriate schema based on the nbformat_minor field. Defaults to v4.5 schema if
   * nbformat_minor is missing or out of supported range.
   *
   * @param nbformatMinor The nbformat_minor value from the notebook
   * @return The appropriate JsonSchema for validation
   */
  private JsonSchema selectSchema(Integer nbformatMinor) {
    // If nbformat_minor is null, use default
    if (nbformatMinor == null) {
      log.info(
          new StringMapMessage()
              .with(MESSAGE, "nbformat_minor not found, using default schema")
              .with("DefaultVersion", "4." + DEFAULT_MINOR_VERSION));
      return schemasByMinorVersion.get(DEFAULT_MINOR_VERSION);
    }

    // If nbformat_minor is outside our supported range, use default
    if (nbformatMinor < MIN_MINOR_VERSION || nbformatMinor > MAX_MINOR_VERSION) {
      log.info(
          new StringMapMessage()
              .with(MESSAGE, "nbformat_minor outside supported range, using default schema")
              .with("RequestedMinorVersion", String.valueOf(nbformatMinor))
              .with("SupportedRange", MIN_MINOR_VERSION + " to " + MAX_MINOR_VERSION)
              .with("DefaultVersion", "4." + DEFAULT_MINOR_VERSION));
      return schemasByMinorVersion.get(DEFAULT_MINOR_VERSION);
    }

    // Try to get the exact schema version
    JsonSchema schema = schemasByMinorVersion.get(nbformatMinor);
    if (schema != null) {
      log.debug(
          new StringMapMessage()
              .with(MESSAGE, "Using schema for nbformat version")
              .with("MinorVersion", String.valueOf(nbformatMinor)));
      return schema;
    }

    // Fallback to default if somehow the schema wasn't loaded
    log.warn(
        new StringMapMessage()
            .with(MESSAGE, "Schema not available for minor version, using default")
            .with("RequestedMinorVersion", String.valueOf(nbformatMinor))
            .with("DefaultVersion", "4." + DEFAULT_MINOR_VERSION));
    return schemasByMinorVersion.get(DEFAULT_MINOR_VERSION);
  }

  /**
   * Validates a Jupyter Notebook JSON string against the appropriate nbformat v4 schema.
   * Automatically selects the correct schema based on the notebook's nbformat_minor field.
   *
   * @param notebookJson The notebook JSON string to validate
   * @return true if the notebook is valid, false otherwise
   */
  public boolean validateNotebook(String notebookJson) {
    try {
      JsonNode jsonNode = objectMapper.readTree(notebookJson);

      // Extract nbformat_minor and select appropriate schema
      Integer nbformatMinor = extractNbformatMinor(jsonNode);
      JsonSchema schema = selectSchema(nbformatMinor);

      Set<ValidationMessage> errors = schema.validate(jsonNode);

      if (errors.isEmpty()) {
        log.debug(
            new StringMapMessage()
                .with(MESSAGE, "Notebook validation passed")
                .with("NbformatMinor", String.valueOf(nbformatMinor)));
        return true;
      } else {
        log.warn(
            new StringMapMessage()
                .with(MESSAGE, "Notebook validation failed")
                .with("NbformatMinor", String.valueOf(nbformatMinor))
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
