package org.jupytereverywhere.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class CellDTOTest {

  @Test
  void testGettersAndSetters() {
    CellDTO cell = new CellDTO();

    cell.setCellType("code");
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("author", "John Doe");
    cell.setMetadata(metadata);
    List<String> source = Arrays.asList("print('Hello World')");
    cell.setSource(source);
    cell.setExecutionCount(1);
    OutputDTO output = new OutputDTO();
    output.setName("stdout");
    output.setText(Arrays.asList("Hello World\n"));
    List<OutputDTO> outputs = Arrays.asList(output);
    cell.setOutputs(outputs);

    assertEquals("code", cell.getCellType());
    assertEquals(metadata, cell.getMetadata());
    assertEquals(source, cell.getSource());
    assertEquals(1, cell.getExecutionCount());
    assertEquals(outputs, cell.getOutputs());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    CellDTO cell = new CellDTO();
    cell.setCellType("code");
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("author", "John Doe");
    cell.setMetadata(metadata);
    List<String> source = Arrays.asList("print('Hello World')");
    cell.setSource(source);
    cell.setExecutionCount(1);
    OutputDTO output = new OutputDTO();
    output.setName("stdout");
    output.setText(Arrays.asList("Hello World\n"));
    List<OutputDTO> outputs = Arrays.asList(output);
    cell.setOutputs(outputs);

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(cell);

    assertTrue(json.contains("\"cell_type\":\"code\""));
    assertTrue(json.contains("\"metadata\":{\"author\":\"John Doe\"}"));
    assertTrue(json.contains("\"source\":[\"print('Hello World')\"]"));
    assertTrue(json.contains("\"execution_count\":1"));
    assertTrue(json.contains("\"outputs\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json =
        "{\"cell_type\":\"code\",\"metadata\":{\"author\":\"John Doe\"},\"source\":[\"print('Hello World')\"],\"execution_count\":1,\"outputs\":[{\"name\":\"stdout\",\"text\":[\"Hello World\\n\"]}]}";

    ObjectMapper objectMapper = new ObjectMapper();
    CellDTO cell = objectMapper.readValue(json, CellDTO.class);

    assertEquals("code", cell.getCellType());
    Map<String, Object> expectedMetadata = new HashMap<>();
    expectedMetadata.put("author", "John Doe");
    assertEquals(expectedMetadata, cell.getMetadata());
    assertEquals(Arrays.asList("print('Hello World')"), cell.getSource());
    assertEquals(1, cell.getExecutionCount());
    assertNotNull(cell.getOutputs());
    assertEquals(1, cell.getOutputs().size());
    OutputDTO output = cell.getOutputs().get(0);
    assertEquals("stdout", output.getName());
    assertEquals(Arrays.asList("Hello World\n"), output.getText());
  }

  @Test
  void testEqualsAndHashCode() {
    CellDTO cell1 = new CellDTO();
    cell1.setCellType("code");
    cell1.setExecutionCount(1);

    CellDTO cell2 = new CellDTO();
    cell2.setCellType("code");
    cell2.setExecutionCount(1);

    assertEquals(cell1, cell2);
    assertEquals(cell1.hashCode(), cell2.hashCode());

    cell2.setCellType("markdown");

    assertNotEquals(cell1, cell2);
  }

  @Test
  public void testEquals_SameObject() {
    CellDTO cell = new CellDTO();
    assertTrue(cell.equals(cell));
  }

  @Test
  public void testEquals_NullObject() {
    CellDTO cell = new CellDTO();
    assertFalse(cell.equals(null));
  }

  @Test
  public void testEquals_DifferentClass() {
    CellDTO cell = new CellDTO();
    Object differentObject = "Not a CellDTO";
    assertFalse(cell.equals(differentObject));
  }

  @Test
  public void testEquals_DifferentProperties() {
    CellDTO cell1 = new CellDTO();
    cell1.setCellType("code");
    cell1.setExecutionCount(1);

    CellDTO cell2 = new CellDTO();
    cell2.setCellType("code");
    cell2.setExecutionCount(2);

    assertFalse(cell1.equals(cell2));
  }

  @Test
  public void testEquals_SameProperties() {
    CellDTO cell1 = new CellDTO();
    cell1.setCellType("code");
    cell1.setExecutionCount(1);

    CellDTO cell2 = new CellDTO();
    cell2.setCellType("code");
    cell2.setExecutionCount(1);

    assertTrue(cell1.equals(cell2));
  }

  @Test
  public void testHashCode_SameProperties() {
    CellDTO cell1 = new CellDTO();
    cell1.setCellType("code");
    cell1.setExecutionCount(1);

    CellDTO cell2 = new CellDTO();
    cell2.setCellType("code");
    cell2.setExecutionCount(1);

    assertEquals(cell1.hashCode(), cell2.hashCode());
  }

  @Test
  public void testHashCode_DifferentProperties() {
    CellDTO cell1 = new CellDTO();
    cell1.setCellType("code");
    cell1.setExecutionCount(1);

    CellDTO cell2 = new CellDTO();
    cell2.setCellType("markdown");
    cell2.setExecutionCount(1);

    assertNotEquals(cell1.hashCode(), cell2.hashCode());
  }

  @Test
  public void testToString() {
    CellDTO cell = new CellDTO();
    cell.setCellType("code");
    cell.setExecutionCount(1);
    String toStringResult = cell.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("cellType=code"));
    assertTrue(toStringResult.contains("executionCount=1"));
  }
}
