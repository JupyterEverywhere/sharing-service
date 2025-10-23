package org.jupytereverywhere.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class KernelspecDTOTest {

  @Test
  void testConstructorAndGetters() {
    KernelspecDTO kernelspec = new KernelspecDTO("Python 3", "python", "python3");

    assertEquals("Python 3", kernelspec.getDisplayName());
    assertEquals("python", kernelspec.getLanguage());
    assertEquals("python3", kernelspec.getName());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    KernelspecDTO kernelspec = new KernelspecDTO("Python 3", "python", "python3");

    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(kernelspec);

    assertTrue(json.contains("\"display_name\":\"Python 3\""));
    assertTrue(json.contains("\"language\":\"python\""));
    assertTrue(json.contains("\"name\":\"python3\""));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json = "{\"display_name\":\"Python 3\",\"language\":\"python\",\"name\":\"python3\"}";

    ObjectMapper objectMapper = new ObjectMapper();
    KernelspecDTO kernelspec = objectMapper.readValue(json, KernelspecDTO.class);

    assertEquals("Python 3", kernelspec.getDisplayName());
    assertEquals("python", kernelspec.getLanguage());
    assertEquals("python3", kernelspec.getName());
  }

  @Test
  void testEqualsAndHashCode() {
    KernelspecDTO ks1 = new KernelspecDTO("python3", "Python 3", "python");
    KernelspecDTO ks2 = new KernelspecDTO("python3", "Python 3", "python");

    assertEquals(ks1, ks2);
    assertEquals(ks1.hashCode(), ks2.hashCode());

    KernelspecDTO ks3 = new KernelspecDTO("python2", "Python 2", "python");
    assertNotEquals(ks1, ks3);
  }

  @Test
  void testEquals_SameObject() {
    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    assertTrue(kernelspec.equals(kernelspec));
  }

  @Test
  void testEquals_NullObject() {
    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    assertFalse(kernelspec.equals(null));
  }

  @Test
  void testEquals_DifferentClass() {
    KernelspecDTO kernelspec = new KernelspecDTO("python3", "Python 3", "python");
    Object otherObject = "Not a KernelspecDTO";
    assertFalse(kernelspec.equals(otherObject));
  }

  @Test
  void testEquals_DifferentProperties() {
    KernelspecDTO ks1 = new KernelspecDTO("python3", "Python 3", "python");
    KernelspecDTO ks2 = new KernelspecDTO("python2", "Python 2", "python");

    assertFalse(ks1.equals(ks2));
  }

  @Test
  void testEquals_SameProperties() {
    KernelspecDTO ks1 = new KernelspecDTO("python3", "Python 3", "python");
    KernelspecDTO ks2 = new KernelspecDTO("python3", "Python 3", "python");

    assertTrue(ks1.equals(ks2));
  }

  @Test
  void testHashCode_SameProperties() {
    KernelspecDTO ks1 = new KernelspecDTO("python3", "Python 3", "python");
    KernelspecDTO ks2 = new KernelspecDTO("python3", "Python 3", "python");

    assertEquals(ks1.hashCode(), ks2.hashCode());
  }

  @Test
  void testHashCode_DifferentProperties() {
    KernelspecDTO ks1 = new KernelspecDTO("python3", "Python 3", "python");
    KernelspecDTO ks2 = new KernelspecDTO("python2", "Python 2", "python");

    assertNotEquals(ks1.hashCode(), ks2.hashCode());
  }

  @Test
  void testToString() {
    KernelspecDTO kernelspec = new KernelspecDTO("Python 3", "python", "python3");

    String toStringResult = kernelspec.toString();
    assertNotNull(toStringResult);
    assertTrue(toStringResult.contains("name=python3"));
  }
}
