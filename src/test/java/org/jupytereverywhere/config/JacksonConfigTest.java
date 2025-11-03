package org.jupytereverywhere.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.dto.LanguageInfoDTO;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonConfigTest {

  private ObjectMapper objectMapper;
  private JacksonConfig jacksonConfig;

  @BeforeEach
  void setUp() {
    jacksonConfig = new JacksonConfig();
    objectMapper = jacksonConfig.objectMapper();
  }

  @Test
  void testObjectMapper_Configuration_Applied() {
    assertNotNull(objectMapper);
    assertTrue(
        objectMapper
            .getDeserializationConfig()
            .isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
    assertTrue(
        objectMapper
            .getDeserializationConfig()
            .isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));
    assertTrue(
        objectMapper
            .getDeserializationConfig()
            .isEnabled(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES));
  }

  @Test
  void testDeserialization_MissingNotebook_ProducesNull() throws Exception {
    String json = "{\"password\": \"test\"}";

    JupyterNotebookRequest request = objectMapper.readValue(json, JupyterNotebookRequest.class);

    assertNotNull(request);
    assertEquals("test", request.getPassword());
    assertNull(request.getNotebook());
  }

  @Test
  void testDeserialization_ValidRequest_Succeeds() throws Exception {
    String json =
        """
      {
        "password": "test",
        "notebook": {
          "nbformat": 4,
          "nbformat_minor": 4,
          "metadata": {"kernelspec": {"name": "python3"}},
          "cells": []
        }
      }
      """;

    JupyterNotebookRequest request = objectMapper.readValue(json, JupyterNotebookRequest.class);

    assertNotNull(request);
    assertNotNull(request.getNotebook());
    assertEquals(4, request.getNotebook().getNbformat());
  }

  @Test
  void testSerialization_NON_NULL_Configuration() throws Exception {
    // Create LanguageInfoDTO with some null fields and some non-null fields
    LanguageInfoDTO langInfo = new LanguageInfoDTO();
    langInfo.setName(null); // Null field - should NOT be serialized
    langInfo.setVersion("3.9.0"); // Non-null field - should be serialized
    langInfo.setMimetype(null); // Another null field - should NOT be serialized
    langInfo.setFileExtension(".py"); // Non-null field - should be serialized

    String json = objectMapper.writeValueAsString(langInfo);

    // Verify null fields are NOT in the JSON
    assertFalse(
        json.contains("\"name\""),
        "Null 'name' field should not be serialized with NON_NULL configuration");
    assertFalse(
        json.contains("\"mimetype\""),
        "Null 'mimetype' field should not be serialized with NON_NULL configuration");

    // Verify non-null fields ARE in the JSON
    assertTrue(
        json.contains("\"version\":\"3.9.0\""), "Non-null 'version' field should be serialized");
    assertTrue(
        json.contains("\"file_extension\":\".py\""),
        "Non-null 'file_extension' field should be serialized");
  }
}
