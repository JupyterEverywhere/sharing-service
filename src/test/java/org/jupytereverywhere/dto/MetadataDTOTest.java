package org.jupytereverywhere.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.jupytereverywhere.dto.CodemirrorModeDTO;
import org.jupytereverywhere.dto.KernelspecDTO;
import org.jupytereverywhere.dto.LanguageInfoDTO;
import org.jupytereverywhere.dto.MetadataDTO;

class MetadataDTOTest {

  @Test
  void testConstructorAndGetters() {
    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    LanguageInfoDTO languageInfo = new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5");

    MetadataDTO metadata = new MetadataDTO(kernelspec, languageInfo);

    assertEquals(kernelspec, metadata.getKernelspec());
    assertEquals(languageInfo, metadata.getLanguageInfo());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    LanguageInfoDTO languageInfo = new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5");

    MetadataDTO metadata = new MetadataDTO(kernelspec, languageInfo);

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(metadata);

    assertTrue(json.contains("\"kernelspec\""));
    assertTrue(json.contains("\"language_info\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json = "{\"kernelspec\":{\"name\":\"python3\",\"display_name\":\"Python 3\",\"language\":\"python\"},"
        + "\"language_info\":{\"codemirror_mode\":{\"name\":\"python\",\"version\":3},"
        + "\"file_extension\":\".py\",\"mimetype\":\"text/x-python\",\"name\":\"python\","
        + "\"nbconvert_exporter\":\"python\",\"version\":\"3.8.5\"}}";

    ObjectMapper objectMapper = new ObjectMapper();
    MetadataDTO metadata = objectMapper.readValue(json, MetadataDTO.class);

    assertNotNull(metadata.getKernelspec());
    assertEquals("Python 3", metadata.getKernelspec().getDisplayName());
    assertEquals("python", metadata.getKernelspec().getLanguage());
    assertEquals("python3", metadata.getKernelspec().getName());

    assertNotNull(metadata.getLanguageInfo());
    assertEquals("python", metadata.getLanguageInfo().getName());
    assertEquals("3.8.5", metadata.getLanguageInfo().getVersion());
  }

  @Test
  void testEqualsAndHashCode() {
    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    LanguageInfoDTO languageInfo = new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5");

    MetadataDTO metadata1 = new MetadataDTO(kernelspec, languageInfo);
    MetadataDTO metadata2 = new MetadataDTO(kernelspec, languageInfo);

    assertEquals(metadata1, metadata2);
    assertEquals(metadata1.hashCode(), metadata2.hashCode());
  }

  @Test
  void testHashCode_SameProperties() {
    MetadataDTO metadata1 = new MetadataDTO(new KernelspecDTO("python3", "Python 3", "python"), new LanguageInfoDTO());
    MetadataDTO metadata2 = new MetadataDTO(new KernelspecDTO("python3", "Python 3", "python"), new LanguageInfoDTO());
    assertEquals(metadata1.hashCode(), metadata2.hashCode());
  }

  @Test
  void testEquals_DifferentProperties() {
    MetadataDTO metadata1 = new MetadataDTO(
        new KernelspecDTO("python3", "Python 3", "python"),
        new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5")
    );

    MetadataDTO metadata2 = new MetadataDTO(
        new KernelspecDTO("python3", "Python 3", "python"),
        new LanguageInfoDTO(new CodemirrorModeDTO("java", 8), ".java", "text/x-java", "java", "java", "1.8")
    );

    assertNotEquals(metadata1, metadata2);
  }

  @Test
  void testHashCode_DifferentProperties() {
    MetadataDTO metadata1 = new MetadataDTO(
        new KernelspecDTO("python3", "Python 3", "python"),
        new LanguageInfoDTO(new CodemirrorModeDTO("python", 3), ".py", "text/x-python", "python", "python", "3.8.5")
    );

    MetadataDTO metadata2 = new MetadataDTO(
        new KernelspecDTO("python3", "Python 3", "python"),
        new LanguageInfoDTO(new CodemirrorModeDTO("java", 8), ".java", "text/x-java", "java", "java", "1.8")
    );

    assertNotEquals(metadata1.hashCode(), metadata2.hashCode());
  }

  @Test
  void testToString() {
    MetadataDTO metadata = new MetadataDTO(new KernelspecDTO("python3", "Python 3", "python"), new LanguageInfoDTO());
    String toStringResult = metadata.toString();
    assertNotNull(toStringResult);
  }
}
