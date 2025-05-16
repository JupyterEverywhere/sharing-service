package org.jupytereverywhere.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputDTOTest {

  @Test
  void testGettersAndSetters() {
    OutputDTO output = new OutputDTO();
    output.setOutputType("stream");
    output.setText(Arrays.asList("Hello World\n"));
    output.setName("stdout");

    Map<String, Object> data = new HashMap<>();
    data.put("text/plain", "Hello World");
    output.setData(data);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("key", "value");
    output.setMetadata(metadata);

    output.setEname("NameError");
    output.setEvalue("name 'x' is not defined");
    output.setTraceback(Arrays.asList("Traceback (most recent call last):", "NameError: name 'x' is not defined"));

    assertEquals("stream", output.getOutputType());
    assertEquals(Arrays.asList("Hello World\n"), output.getText());
    assertEquals("stdout", output.getName());
    assertEquals(data, output.getData());
    assertEquals(metadata, output.getMetadata());
    assertEquals("NameError", output.getEname());
    assertEquals("name 'x' is not defined", output.getEvalue());
    assertEquals(Arrays.asList("Traceback (most recent call last):", "NameError: name 'x' is not defined"), output.getTraceback());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    OutputDTO output = new OutputDTO();
    output.setOutputType("stream");
    output.setText(Arrays.asList("Hello World\n"));
    output.setName("stdout");

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(output);

    assertTrue(json.contains("\"output_type\":\"stream\""));
    assertTrue(json.contains("\"text\":[\"Hello World\\n\"]"));
    assertTrue(json.contains("\"name\":\"stdout\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json = "{\"output_type\":\"stream\",\"text\":[\"Hello World\\n\"],\"name\":\"stdout\"}";

    ObjectMapper objectMapper = new ObjectMapper();
    OutputDTO output = objectMapper.readValue(json, OutputDTO.class);

    assertEquals("stream", output.getOutputType());
    assertEquals(Arrays.asList("Hello World\n"), output.getText());
    assertEquals("stdout", output.getName());
  }

  @Test
  void testEqualsAndHashCode() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");
    output1.setName("stdout");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("stream");
    output2.setName("stdout");

    assertEquals(output1, output2);
    assertEquals(output1.hashCode(), output2.hashCode());

    output2.setName("stderr");
    assertNotEquals(output1, output2);
  }

  @Test
  public void testEquals_SameObject() {
    OutputDTO output = new OutputDTO();
    assertTrue(output.equals(output));
  }

  @Test
  public void testEquals_NullObject() {
    OutputDTO output = new OutputDTO();
    assertFalse(output.equals(null));
  }

  @Test
  public void testEquals_DifferentClass() {
    OutputDTO output = new OutputDTO();
    Object otherObject = "Not an OutputDTO";
    assertFalse(output.equals(otherObject));
  }

  @Test
  public void testEquals_DifferentProperties() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("error");

    assertFalse(output1.equals(output2));
  }

  @Test
  public void testEquals_SameProperties() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("stream");

    assertTrue(output1.equals(output2));
  }

  @Test
  public void testHashCode_SameProperties() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("stream");

    assertEquals(output1.hashCode(), output2.hashCode());
  }

  @Test
  public void testHashCode_DifferentProperties() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("error");

    assertNotEquals(output1.hashCode(), output2.hashCode());
  }

  @Test
  public void testToString() {
    OutputDTO output = new OutputDTO();
    output.setOutputType("stream");

    String toStringResult = output.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("outputType=stream"));
  }

  @Test
  void testEquals_BothObjectsHaveNullFields() {
    OutputDTO output1 = new OutputDTO();
    OutputDTO output2 = new OutputDTO();

    assertTrue(output1.equals(output2), "Both objects with all fields null should be equal");
  }

  @Test
  void testEquals_OneObjectHasNullFields() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");

    OutputDTO output2 = new OutputDTO();

    assertFalse(output1.equals(output2), "Objects with different values in outputType should not be equal");
  }

  @Test
  void testEquals_BothObjectsHaveSomeNullFields() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");
    output1.setName("stdout");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("stream");

    assertFalse(output1.equals(output2), "Objects with different values in name should not be equal");
  }

  @Test
  void testEquals_AllFieldsDifferent() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");
    output1.setText(Arrays.asList("Hello World\n"));
    output1.setData(new HashMap<>());
    output1.setMetadata(new HashMap<>());
    output1.setName("stdout");
    output1.setEname("NameError");
    output1.setEvalue("name 'x' is not defined");
    output1.setTraceback(Arrays.asList("Traceback (most recent call last):", "NameError: name 'x' is not defined"));

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("error");
    output2.setText(Arrays.asList("Error occurred\n"));
    output2.setData(new HashMap<>());
    output2.setMetadata(new HashMap<>());
    output2.setName("stderr");
    output2.setEname("TypeError");
    output2.setEvalue("unsupported operand type(s)");
    output2.setTraceback(Arrays.asList("Traceback (most recent call last):", "TypeError: unsupported operand type(s)"));

    assertFalse(output1.equals(output2), "Objects with all fields different should not be equal");
  }

  @Test
  void testEquals_SomeFieldsDifferent() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");
    output1.setName("stdout");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("stream");
    output2.setName("stderr");

    assertFalse(output1.equals(output2), "Objects with different values in name should not be equal");
  }

  @Test
  void testEquals_DifferentText() {
    OutputDTO output1 = new OutputDTO();
    output1.setText(Arrays.asList("Hello World\n"));

    OutputDTO output2 = new OutputDTO();
    output2.setText(Arrays.asList("Goodbye World\n"));

    assertFalse(output1.equals(output2), "Objects with different text lists should not be equal");
  }

  @Test
  void testEquals_EmptyListVsNull() {
    OutputDTO output1 = new OutputDTO();
    output1.setText(Collections.emptyList());

    OutputDTO output2 = new OutputDTO();
    output2.setText(null);

    assertFalse(output1.equals(output2), "An empty list and a null list should not be equal");
  }

  @Test
  void testHashCode_BothObjectsHaveNullFields() {
    OutputDTO output1 = new OutputDTO();
    OutputDTO output2 = new OutputDTO();

    assertEquals(output1.hashCode(), output2.hashCode(), "Objects with all fields null should have the same hashCode");
  }

  @Test
  void testHashCode_OneObjectHasNullFields() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");

    OutputDTO output2 = new OutputDTO();

    assertNotEquals(output1.hashCode(), output2.hashCode(), "Objects with different values in outputType should have different hashCodes");
  }

  @Test
  void testHashCode_SomeFieldsNull() {
    OutputDTO output1 = new OutputDTO();
    output1.setOutputType("stream");
    output1.setName("stdout");

    OutputDTO output2 = new OutputDTO();
    output2.setOutputType("stream");

    assertNotEquals(output1.hashCode(), output2.hashCode(), "Objects with different values in name should have different hashCodes");
  }
}
