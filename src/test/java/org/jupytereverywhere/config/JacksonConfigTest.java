package org.jupytereverywhere.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;

import static org.junit.jupiter.api.Assertions.*;

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
    assertTrue(objectMapper.getDeserializationConfig()
        .isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES));
    assertTrue(objectMapper.getDeserializationConfig()
        .isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS));
    assertTrue(objectMapper.getDeserializationConfig()
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
    String json = """
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
}
