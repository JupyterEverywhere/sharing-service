package org.jupytereverywhere.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.dto.CodemirrorModeDTO;

import static org.junit.jupiter.api.Assertions.*;

class CodemirrorModeDTOTest {

  @Test
  void testConstructorAndGetters() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);

    assertEquals("python", mode.getName());
    assertEquals(3, mode.getVersion());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(mode);

    assertTrue(json.contains("\"name\":\"python\""));
    assertTrue(json.contains("\"version\":3"));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json = "{\"name\":\"python\",\"version\":3}";

    ObjectMapper objectMapper = new ObjectMapper();
    CodemirrorModeDTO mode = objectMapper.readValue(json, CodemirrorModeDTO.class);

    assertEquals("python", mode.getName());
    assertEquals(3, mode.getVersion());
  }

  @Test
  void testEqualsAndHashCode() {
    CodemirrorModeDTO mode1 = new CodemirrorModeDTO("python", 3);
    CodemirrorModeDTO mode2 = new CodemirrorModeDTO("python", 3);

    assertEquals(mode1, mode2);
    assertEquals(mode1.hashCode(), mode2.hashCode());

    CodemirrorModeDTO mode3 = new CodemirrorModeDTO("python", 2);
    assertNotEquals(mode1, mode3);
  }

  @Test
  public void testEquals_SameObject() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    assertTrue(mode.equals(mode));
  }

  @Test
  public void testEquals_NullObject() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    assertFalse(mode.equals(null));
  }

  @Test
  public void testEquals_DifferentClass() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    assertFalse(mode.equals("Not a CodemirrorModeDTO"));
  }

  @Test
  public void testEquals_DifferentProperties() {
    CodemirrorModeDTO mode1 = new CodemirrorModeDTO("python", 3);
    CodemirrorModeDTO mode2 = new CodemirrorModeDTO("java", 8);

    assertFalse(mode1.equals(mode2));
  }

  @Test
  public void testEquals_SameProperties() {
    CodemirrorModeDTO mode1 = new CodemirrorModeDTO("python", 3);
    CodemirrorModeDTO mode2 = new CodemirrorModeDTO("python", 3);

    assertTrue(mode1.equals(mode2));
  }

  @Test
  public void testHashCode_SameProperties() {
    CodemirrorModeDTO mode1 = new CodemirrorModeDTO("python", 3);
    CodemirrorModeDTO mode2 = new CodemirrorModeDTO("python", 3);

    assertEquals(mode1.hashCode(), mode2.hashCode());
  }

  @Test
  public void testHashCode_DifferentProperties() {
    CodemirrorModeDTO mode1 = new CodemirrorModeDTO("python", 3);
    CodemirrorModeDTO mode2 = new CodemirrorModeDTO("java", 8);

    assertNotEquals(mode1.hashCode(), mode2.hashCode());
  }

  @Test
  public void testToString() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);

    String toStringResult = mode.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("name=python"));
    assertTrue(toStringResult.contains("version=3"));
  }
}
