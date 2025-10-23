package org.jupytereverywhere.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JupyterNotebookEntityTest {

  private JupyterNotebookEntity notebook;

  @BeforeEach
  public void setUp() {
    notebook = new JupyterNotebookEntity();
  }

  @Test
  void testSetAndGetId() {
    UUID id = UUID.randomUUID();
    notebook.setId(id);
    assertEquals(id, notebook.getId());
  }

  @Test
  void testSetAndGetSessionId() {
    UUID sessionId = UUID.randomUUID();
    notebook.setSessionId(sessionId);
    assertEquals(sessionId, notebook.getSessionId());
  }

  @Test
  void testSetAndGetKernelName() {
    String kernelName = "Python 3";
    notebook.setKernelName(kernelName);
    assertEquals(kernelName, notebook.getKernelName());
  }

  @Test
  void testSetAndGetKernelDisplayName() {
    String kernelDisplayName = "Python 3.8";
    notebook.setKernelDisplayName(kernelDisplayName);
    assertEquals(kernelDisplayName, notebook.getKernelDisplayName());
  }

  @Test
  void testSetAndGetLanguage() {
    String language = "Python";
    notebook.setLanguage(language);
    assertEquals(language, notebook.getLanguage());
  }

  @Test
  void testSetAndGetLanguageVersion() {
    String languageVersion = "3.8";
    notebook.setLanguageVersion(languageVersion);
    assertEquals(languageVersion, notebook.getLanguageVersion());
  }

  @Test
  void testSetAndGetFileExtension() {
    String fileExtension = ".ipynb";
    notebook.setFileExtension(fileExtension);
    assertEquals(fileExtension, notebook.getFileExtension());
  }

  @Test
  void testSetAndGetStorageUrl() {
    String storageUrl = "s3://bucket/notebook.ipynb";
    notebook.setStorageUrl(storageUrl);
    assertEquals(storageUrl, notebook.getStorageUrl());
  }

  @Test
  void testSetAndGetCreatedAt() {
    Timestamp createdAt = Timestamp.from(Instant.now());
    notebook.setCreatedAt(createdAt);
    assertEquals(createdAt, notebook.getCreatedAt());
  }

  @Test
  void testEquals() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    notebook1.setId(UUID.randomUUID());
    notebook2.setId(notebook1.getId());

    assertTrue(notebook1.equals(notebook2));
    assertEquals(notebook1, notebook2);
  }

  @Test
  void testHashCode() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    notebook1.setId(UUID.randomUUID());
    notebook2.setId(notebook1.getId());

    assertEquals(notebook1.hashCode(), notebook2.hashCode());
  }

  @Test
  void testToString() {
    UUID id = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    notebook.setId(id);
    notebook.setSessionId(sessionId);
    notebook.setKernelName("Python 3");
    notebook.setDomain(null);
    notebook.setReadableId(null);

    String expectedString =
        "JupyterNotebookEntity(id="
            + notebook.getId()
            + ", sessionId="
            + notebook.getSessionId()
            + ", kernelName=Python 3"
            + ", kernelDisplayName=null"
            + ", language=null"
            + ", languageVersion=null"
            + ", fileExtension=null"
            + ", domain=null"
            + ", storageUrl=null"
            + ", readableId=null"
            + ", createdAt=null)";

    assertEquals(expectedString, notebook.toString());
  }

  @Test
  void testCanEqual() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    assertTrue(notebook1.canEqual(notebook2));
  }

  @Test
  void testEquals_SameObject() {
    assertTrue(notebook.equals(notebook), "The same object should be equal to itself");
  }

  @Test
  void testEquals_EquivalentObject() {
    JupyterNotebookEntity anotherNotebook = new JupyterNotebookEntity();
    anotherNotebook.setId(notebook.getId());
    anotherNotebook.setSessionId(notebook.getSessionId());
    anotherNotebook.setKernelName(notebook.getKernelName());
    anotherNotebook.setKernelDisplayName(notebook.getKernelDisplayName());
    anotherNotebook.setLanguage(notebook.getLanguage());
    anotherNotebook.setLanguageVersion(notebook.getLanguageVersion());
    anotherNotebook.setFileExtension(notebook.getFileExtension());
    anotherNotebook.setStorageUrl(notebook.getStorageUrl());
    anotherNotebook.setCreatedAt(notebook.getCreatedAt());

    assertTrue(
        notebook.equals(anotherNotebook), "Two objects with the same values should be equal");
  }

  @Test
  void testEquals_DifferentObject() {
    JupyterNotebookEntity differentNotebook = new JupyterNotebookEntity();
    differentNotebook.setId(UUID.randomUUID());

    assertFalse(
        notebook.equals(differentNotebook), "Two objects with different IDs should not be equal");
  }

  @Test
  void testEquals_NullObject() {
    assertFalse(notebook.equals(null), "The object should not be equal to null");
  }

  @Test
  void testEquals_DifferentType() {
    Object otherObject = "Not a JupyterNotebookEntity";
    assertFalse(
        notebook.equals(otherObject),
        "The object should not be equal to an object of a different type");
  }

  @Test
  void testHashCode_EqualObjects() {
    JupyterNotebookEntity anotherNotebook = new JupyterNotebookEntity();
    anotherNotebook.setId(notebook.getId());
    anotherNotebook.setSessionId(notebook.getSessionId());
    anotherNotebook.setKernelName(notebook.getKernelName());
    anotherNotebook.setKernelDisplayName(notebook.getKernelDisplayName());
    anotherNotebook.setLanguage(notebook.getLanguage());
    anotherNotebook.setLanguageVersion(notebook.getLanguageVersion());
    anotherNotebook.setFileExtension(notebook.getFileExtension());
    anotherNotebook.setStorageUrl(notebook.getStorageUrl());
    anotherNotebook.setCreatedAt(notebook.getCreatedAt());

    assertEquals(
        notebook.hashCode(),
        anotherNotebook.hashCode(),
        "Equal objects must have the same hashCode");
  }

  @Test
  void testHashCode_DifferentObjects() {
    JupyterNotebookEntity differentNotebook = new JupyterNotebookEntity();
    differentNotebook.setId(UUID.randomUUID());

    assertNotEquals(
        notebook.hashCode(),
        differentNotebook.hashCode(),
        "Different objects should have different hashCodes");
  }

  @Test
  void testEquals_WithNullFields() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    assertTrue(notebook1.equals(notebook2), "Both objects with null fields should be equal");

    notebook1.setId(UUID.randomUUID());
    assertFalse(notebook1.equals(notebook2), "Objects with different fields should not be equal");

    notebook2.setId(notebook1.getId());
    assertTrue(
        notebook1.equals(notebook2),
        "Objects with the same ID should be equal even if other fields are null");
  }

  @Test
  void testEquals_DifferentSessionId() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    UUID id = UUID.randomUUID();
    notebook1.setId(id);
    notebook2.setId(id);

    notebook1.setSessionId(UUID.randomUUID());
    notebook2.setSessionId(UUID.randomUUID());

    assertFalse(
        notebook1.equals(notebook2), "Objects with different sessionId should not be equal");
  }

  @Test
  void testEquals_DifferentKernelName() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    UUID id = UUID.randomUUID();
    notebook1.setId(id);
    notebook2.setId(id);

    notebook1.setKernelName("Python 3");
    notebook2.setKernelName("Java 11");

    assertFalse(
        notebook1.equals(notebook2), "Objects with different kernelName should not be equal");
  }

  @Test
  void testEquals_CanEqualFalse() {
    JupyterNotebookEntity notebook = new JupyterNotebookEntity();
    Object differentTypeObject = "I am not a JupyterNotebookEntity";

    assertFalse(
        notebook.equals(differentTypeObject),
        "The object should not be equal to an object of a different type");
  }

  @Test
  void testEquals_IdNullInBoth() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    assertTrue(notebook1.equals(notebook2), "Both IDs null should be considered equal");
  }

  @Test
  void testEquals_IdNullInOne() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    UUID id = UUID.randomUUID();
    notebook1.setId(id);

    assertFalse(notebook1.equals(notebook2), "One null ID and one non-null ID should not be equal");
  }

  @Test
  void testHashCode_ConsistencyWithEquals() {
    JupyterNotebookEntity notebook1 = new JupyterNotebookEntity();
    JupyterNotebookEntity notebook2 = new JupyterNotebookEntity();

    UUID id = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    notebook1.setId(id);
    notebook2.setId(id);

    notebook1.setSessionId(sessionId);
    notebook2.setSessionId(sessionId);

    assertEquals(notebook1, notebook2, "Objects should be equal");
    assertEquals(
        notebook1.hashCode(), notebook2.hashCode(), "HashCodes should be equal for equal objects");
  }

  @Test
  void testToString_AllFields() {
    UUID id = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    String kernelName = "Python 3";
    String kernelDisplayName = "Python 3.8";
    String language = "Python";
    String languageVersion = "3.8";
    String fileExtension = ".ipynb";
    String domain = "example.com";
    String storageUrl = "s3://bucket/notebook.ipynb";
    String readableId = "adjective-adjective-animal";
    Timestamp createdAt = Timestamp.from(Instant.now());

    notebook.setId(id);
    notebook.setSessionId(sessionId);
    notebook.setKernelName(kernelName);
    notebook.setKernelDisplayName(kernelDisplayName);
    notebook.setLanguage(language);
    notebook.setLanguageVersion(languageVersion);
    notebook.setFileExtension(fileExtension);
    notebook.setDomain(domain);
    notebook.setStorageUrl(storageUrl);
    notebook.setCreatedAt(createdAt);
    notebook.setReadableId(readableId);

    String expectedString =
        "JupyterNotebookEntity(id="
            + id
            + ", sessionId="
            + sessionId
            + ", kernelName="
            + kernelName
            + ", kernelDisplayName="
            + kernelDisplayName
            + ", language="
            + language
            + ", languageVersion="
            + languageVersion
            + ", fileExtension="
            + fileExtension
            + ", domain="
            + domain
            + ", storageUrl="
            + storageUrl
            + ", readableId="
            + readableId
            + ", createdAt="
            + createdAt
            + ")";

    assertEquals(
        expectedString,
        notebook.toString(),
        "The toString method should reflect all fields correctly");
  }
}
