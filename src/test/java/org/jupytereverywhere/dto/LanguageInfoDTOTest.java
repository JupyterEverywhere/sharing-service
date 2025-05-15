package org.jupytereverywhere.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.dto.CodemirrorModeDTO;
import org.jupytereverywhere.dto.LanguageInfoDTO;

import static org.junit.jupiter.api.Assertions.*;

class LanguageInfoDTOTest {

  @Test
  void testConstructorAndGetters() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");

    assertEquals(mode, langInfo.getCodemirrorMode());
    assertEquals(".py", langInfo.getFileExtension());
    assertEquals("text/x-python", langInfo.getMimetype());
    assertEquals("python", langInfo.getName());
    assertEquals("python", langInfo.getNbconvertExporter());
    assertEquals("3.8.5", langInfo.getVersion());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(langInfo);

    assertTrue(json.contains("\"codemirror_mode\""));
    assertTrue(json.contains("\"file_extension\":\".py\""));
    assertTrue(json.contains("\"mimetype\":\"text/x-python\""));
    assertTrue(json.contains("\"name\":\"python\""));
    assertTrue(json.contains("\"nbconvert_exporter\":\"python\""));
    assertTrue(json.contains("\"version\":\"3.8.5\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json = "{\"codemirror_mode\":{\"name\":\"python\",\"version\":3},\"file_extension\":\".py\",\"mimetype\":\"text/x-python\",\"name\":\"python\",\"nbconvert_exporter\":\"python\",\"version\":\"3.8.5\"}";

    ObjectMapper objectMapper = new ObjectMapper();
    LanguageInfoDTO langInfo = objectMapper.readValue(json, LanguageInfoDTO.class);

    assertNotNull(langInfo.getCodemirrorMode());
    assertEquals("python", langInfo.getCodemirrorMode().getName());
    assertEquals(3, langInfo.getCodemirrorMode().getVersion());
    assertEquals(".py", langInfo.getFileExtension());
    assertEquals("text/x-python", langInfo.getMimetype());
    assertEquals("python", langInfo.getName());
    assertEquals("python", langInfo.getNbconvertExporter());
    assertEquals("3.8.5", langInfo.getVersion());
  }

  @Test
  void testEqualsAndHashCode() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo1 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    LanguageInfoDTO langInfo2 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");

    assertEquals(langInfo1, langInfo2);
    assertEquals(langInfo1.hashCode(), langInfo2.hashCode());

    LanguageInfoDTO langInfo3 = new LanguageInfoDTO(mode, ".java", "text/x-java", "java", "java", "1.8");
    assertNotEquals(langInfo1, langInfo3);
  }

  @Test
  public void testEquals_SameObject() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    assertTrue(langInfo.equals(langInfo));
  }

  @Test
  public void testEquals_NullObject() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    assertFalse(langInfo.equals(null));
  }

  @Test
  public void testEquals_DifferentClass() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    assertFalse(langInfo.equals("Not a LanguageInfoDTO"));
  }

  @Test
  public void testEquals_DifferentProperties() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo1 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    LanguageInfoDTO langInfo2 = new LanguageInfoDTO(mode, ".java", "text/x-java", "java", "java", "1.8");

    assertFalse(langInfo1.equals(langInfo2));
  }

  @Test
  public void testEquals_SameProperties() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo1 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    LanguageInfoDTO langInfo2 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");

    assertTrue(langInfo1.equals(langInfo2));
  }

  @Test
  public void testHashCode_SameProperties() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo1 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    LanguageInfoDTO langInfo2 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");

    assertEquals(langInfo1.hashCode(), langInfo2.hashCode());
  }

  @Test
  public void testHashCode_DifferentProperties() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo1 = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");
    LanguageInfoDTO langInfo2 = new LanguageInfoDTO(mode, ".java", "text/x-java", "java", "java", "1.8");

    assertNotEquals(langInfo1.hashCode(), langInfo2.hashCode());
  }

  @Test
  public void testToString() {
    CodemirrorModeDTO mode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO langInfo = new LanguageInfoDTO(mode, ".py", "text/x-python", "python", "python", "3.8.5");

    String toStringResult = langInfo.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("name=python"));
  }
}
