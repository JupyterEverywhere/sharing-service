package org.coursekata.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotebookDTOTest {

  @Test
  void testGettersAndSetters() {
    NotebookDTO notebook = new NotebookDTO();
    notebook.setNbformat(4);
    notebook.setNbformatMinor(2);

    MetadataDTO metadata = new MetadataDTO();
    notebook.setMetadata(metadata);

    CellDTO cell = new CellDTO();
    cell.setCellType("code");
    cell.setSource(Arrays.asList("print('Hello World')"));
    List<CellDTO> cells = Arrays.asList(cell);
    notebook.setCells(cells);

    assertEquals(4, notebook.getNbformat());
    assertEquals(2, notebook.getNbformatMinor());
    assertEquals(metadata, notebook.getMetadata());
    assertEquals(cells, notebook.getCells());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    NotebookDTO notebook = new NotebookDTO();
    notebook.setNbformat(4);
    notebook.setNbformatMinor(2);

    MetadataDTO metadata = new MetadataDTO();
    notebook.setMetadata(metadata);

    CellDTO cell = new CellDTO();
    cell.setCellType("code");
    cell.setSource(Arrays.asList("print('Hello World')"));
    notebook.setCells(Arrays.asList(cell));

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(notebook);

    assertTrue(json.contains("\"nbformat\":4"));
    assertTrue(json.contains("\"nbformat_minor\":2"));
    assertTrue(json.contains("\"metadata\""));
    assertTrue(json.contains("\"cells\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json = "{\"nbformat\":4,\"nbformat_minor\":2,\"metadata\":{\"kernelspec\":{\"display_name\":\"Python 3\",\"name\":\"python3\"},\"language_info\":{\"name\":\"python\",\"version\":\"3.8.5\",\"file_extension\":\".py\"}},\"cells\":[{\"cell_type\":\"code\",\"source\":[\"print('Hello World')\"]}]}";

    ObjectMapper objectMapper = new ObjectMapper();
    NotebookDTO notebook = objectMapper.readValue(json, NotebookDTO.class);

    assertEquals(4, notebook.getNbformat());
    assertEquals(2, notebook.getNbformatMinor());

    assertNotNull(notebook.getMetadata());
    assertEquals("Python 3", notebook.getMetadata().getKernelspec().getDisplayName());
    assertEquals("python3", notebook.getMetadata().getKernelspec().getName());
    assertEquals("python", notebook.getMetadata().getLanguageInfo().getName());
    assertEquals("3.8.5", notebook.getMetadata().getLanguageInfo().getVersion());
    assertEquals(".py", notebook.getMetadata().getLanguageInfo().getFileExtension());

    assertNotNull(notebook.getCells());
    assertEquals(1, notebook.getCells().size());
    CellDTO cell = notebook.getCells().get(0);
    assertEquals("code", cell.getCellType());
    assertEquals(Arrays.asList("print('Hello World')"), cell.getSource());
  }

  @Test
  void testEqualsAndHashCode() {
    NotebookDTO notebook1 = new NotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);

    NotebookDTO notebook2 = new NotebookDTO();
    notebook2.setNbformat(4);
    notebook2.setNbformatMinor(2);

    assertEquals(notebook1, notebook2);
    assertEquals(notebook1.hashCode(), notebook2.hashCode());

    notebook2.setNbformatMinor(3);
    assertNotEquals(notebook1, notebook2);
  }

  @Test
  public void testEquals_SameObject() {
    NotebookDTO notebook = new NotebookDTO();
    assertTrue(notebook.equals(notebook));
  }

  @Test
  public void testEquals_NullObject() {
    NotebookDTO notebook = new NotebookDTO();
    assertFalse(notebook.equals(null));
  }

  @Test
  public void testEquals_DifferentClass() {
    NotebookDTO notebook = new NotebookDTO();
    assertFalse(notebook.equals("Not a NotebookDTO"));
  }

  @Test
  public void testEquals_DifferentProperties() {
    NotebookDTO notebook1 = new NotebookDTO();
    notebook1.setNbformat(4);

    NotebookDTO notebook2 = new NotebookDTO();
    notebook2.setNbformat(3);

    assertFalse(notebook1.equals(notebook2));
  }

  @Test
  public void testEquals_SameProperties() {
    NotebookDTO notebook1 = new NotebookDTO();
    notebook1.setNbformat(4);

    NotebookDTO notebook2 = new NotebookDTO();
    notebook2.setNbformat(4);

    assertTrue(notebook1.equals(notebook2));
  }

  @Test
  public void testHashCode_SameProperties() {
    NotebookDTO notebook1 = new NotebookDTO();
    notebook1.setNbformat(4);

    NotebookDTO notebook2 = new NotebookDTO();
    notebook2.setNbformat(4);

    assertEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  public void testHashCode_DifferentProperties() {
    NotebookDTO notebook1 = new NotebookDTO();
    notebook1.setNbformat(4);

    NotebookDTO notebook2 = new NotebookDTO();
    notebook2.setNbformat(3);

    assertNotEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  public void testToString() {
    NotebookDTO notebook = new NotebookDTO();
    notebook.setNbformat(4);

    String toStringResult = notebook.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("nbformat=4"));
  }
}
