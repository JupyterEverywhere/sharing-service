package org.jupytereverywhere.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JupyterNotebookDTOTest {

  @Test
  void testGettersAndSetters() {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO();
    notebook.setNbformat(4);
    notebook.setNbformatMinor(2);

    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    LanguageInfoDTO languageInfo = new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5");
    MetadataDTO metadata = new MetadataDTO(kernelspec, languageInfo);
    notebook.setMetadata(metadata);

    Map<String, Object> cell = new HashMap<>();
    cell.put("cell_type", "code");
    cell.put("source", Arrays.asList("print('Hello World')"));
    notebook.setCells(Collections.singletonList(cell));

    assertEquals(4, notebook.getNbformat());
    assertEquals(2, notebook.getNbformatMinor());
    assertEquals(metadata, notebook.getMetadata());
    assertEquals(1, notebook.getCells().size());
    assertEquals(cell, notebook.getCells().get(0));
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO();
    notebook.setNbformat(4);
    notebook.setNbformatMinor(2);

    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    LanguageInfoDTO languageInfo = new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5");
    MetadataDTO metadata = new MetadataDTO(kernelspec, languageInfo);
    notebook.setMetadata(metadata);

    Map<String, Object> cell = new HashMap<>();
    cell.put("cell_type", "code");
    cell.put("source", Arrays.asList("print('Hello World')"));
    notebook.setCells(Collections.singletonList(cell));

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
    JupyterNotebookDTO notebook = objectMapper.readValue(json, JupyterNotebookDTO.class);

    assertEquals(4, notebook.getNbformat());
    assertEquals(2, notebook.getNbformatMinor());

    MetadataDTO metadata = notebook.getMetadata();
    assertNotNull(metadata);
    assertEquals("Python 3", metadata.getKernelspec().getDisplayName());
    assertEquals("python3", metadata.getKernelspec().getName());
    assertEquals("python", metadata.getLanguageInfo().getName());
    assertEquals("3.8.5", metadata.getLanguageInfo().getVersion());
    assertEquals(".py", metadata.getLanguageInfo().getFileExtension());

    assertNotNull(notebook.getCells());
    assertEquals(1, notebook.getCells().size());

    Map<String, Object> cell = notebook.getCells().get(0);
    assertEquals("code", cell.get("cell_type"));
    assertEquals(List.of("print('Hello World')"), cell.get("source"));
  }

  @Test
  void testEqualsAndHashCode() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(4);
    notebook2.setNbformatMinor(2);

    assertEquals(notebook1, notebook2);
    assertEquals(notebook1.hashCode(), notebook2.hashCode());

    notebook2.setNbformatMinor(3);
    assertNotEquals(notebook1, notebook2);
  }

  @Test
  public void testEquals_SameObject() {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO();
    assertTrue(notebook.equals(notebook));
  }

  @Test
  public void testEquals_NullObject() {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO();
    assertFalse(notebook.equals(null));
  }

  @Test
  public void testEquals_DifferentClass() {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO();
    Object otherObject = "Not a JupyterNotebookDTO";
    assertFalse(notebook.equals(otherObject));
  }

  @Test
  public void testEquals_DifferentProperties() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(3);
    notebook2.setNbformatMinor(1);

    assertFalse(notebook1.equals(notebook2));
  }

  @Test
  public void testEquals_SameProperties() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(4);
    notebook2.setNbformatMinor(2);

    assertTrue(notebook1.equals(notebook2));
  }

  @Test
  public void testHashCode_SameProperties() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(4);
    notebook2.setNbformatMinor(2);

    assertEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  public void testHashCode_DifferentProperties() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(3);
    notebook2.setNbformatMinor(1);

    assertNotEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  public void testToString() {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO();
    notebook.setNbformat(4);
    notebook.setNbformatMinor(2);

    String toStringResult = notebook.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("nbformat=4"));
    assertTrue(toStringResult.contains("nbformatMinor=2"));
  }

  @Test
  void testFullConstructor() {
    MetadataDTO metadata = new MetadataDTO(
        new KernelspecDTO("python3", "Python 3", "python"),
        new LanguageInfoDTO(
            new CodemirrorModeDTO("python", 3),
            ".py", "text/x-python", "python", "python", "3.8.5"
        )
    );

    Map<String, Object> cell = new HashMap<>();
    cell.put("cell_type", "code");
    cell.put("source", List.of("print('Hello World')"));

    List<Map<String, Object>> cells = List.of(cell);

    JupyterNotebookDTO notebook = new JupyterNotebookDTO(4, 2, metadata, cells);

    assertEquals(4, notebook.getNbformat());
    assertEquals(2, notebook.getNbformatMinor());
    assertEquals(metadata, notebook.getMetadata());
    assertEquals(cells, notebook.getCells());
  }

  @Test
  void testEqualsAndHashCode_WithDifferentMetadata() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);
    notebook1.setMetadata(new MetadataDTO());

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(4);
    notebook2.setNbformatMinor(2);
    notebook2.setMetadata(null);

    assertNotEquals(notebook1, notebook2);
    assertNotEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  void testEqualsAndHashCode_WithDifferentCells() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO();
    notebook1.setNbformat(4);
    notebook1.setNbformatMinor(2);
    notebook1.setCells(List.of(Map.of("cell_type", "code", "source", List.of("print('Hello World')"))));

    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO();
    notebook2.setNbformat(4);
    notebook2.setNbformatMinor(2);
    notebook2.setCells(null);

    assertNotEquals(notebook1, notebook2);
    assertNotEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  void testHashCode_NullValues() {
    JupyterNotebookDTO notebook = new JupyterNotebookDTO(4, 2, null, null);
    assertNotNull(notebook.hashCode());
  }

  @Test
  void testEquals_NullFields() {
    JupyterNotebookDTO notebook1 = new JupyterNotebookDTO(4, 2, null, null);
    JupyterNotebookDTO notebook2 = new JupyterNotebookDTO(4, 2, null, null);

    assertEquals(notebook1, notebook2);
    assertEquals(notebook1.hashCode(), notebook2.hashCode());
  }
}
