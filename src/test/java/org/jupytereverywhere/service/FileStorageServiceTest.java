package org.jupytereverywhere.service;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupytereverywhere.dto.CodemirrorModeDTO;
import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.dto.KernelspecDTO;
import org.jupytereverywhere.dto.LanguageInfoDTO;
import org.jupytereverywhere.dto.MetadataDTO;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.exception.NotebookStorageException;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

  private FileStorageService fileStorageService;
  private final ObjectMapper objectMapper;

  public FileStorageServiceTest() {
    objectMapper = new ObjectMapper();
    // Configure to match JacksonConfig
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @BeforeEach
  void setUp() {
    fileStorageService = new FileStorageService(objectMapper);
  }

  @Test
  void testSaveNotebook_Success() throws IOException {
    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", "/path/to/notebooks");

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    MetadataDTO metadata =
        new MetadataDTO(
            new KernelspecDTO("python3", "Python 3", "python"),
            new LanguageInfoDTO(
                new CodemirrorModeDTO("python", 3),
                ".py",
                "text/x-python",
                "python",
                "python",
                "3.8.5"));
    notebookDto.setMetadata(metadata);

    String fileName = "testNotebook.ipynb";
    String directoryPath = "/path/to/notebooks";
    Path testPath = Paths.get(directoryPath, fileName);

    String jsonString = objectMapper.writeValueAsString(notebookDto);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock
          .when(() -> Files.createDirectories(testPath.getParent()))
          .thenReturn(testPath.getParent());
      filesMock.when(() -> Files.write(eq(testPath), any(byte[].class))).thenReturn(testPath);

      String resultPath = fileStorageService.uploadNotebook(jsonString, fileName);

      assertNotNull(resultPath);
      assertEquals(testPath.toString(), resultPath);

      filesMock.verify(
          () -> Files.write(eq(testPath), eq(jsonString.getBytes(StandardCharsets.UTF_8))));
    }
  }

  @Test
  void testSaveNotebook_ThrowsIOException() throws JsonProcessingException {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    MetadataDTO metadata =
        new MetadataDTO(
            new KernelspecDTO("python3", "Python 3", "python"),
            new LanguageInfoDTO(
                new CodemirrorModeDTO("python", 3),
                ".py",
                "text/x-python",
                "python",
                "python",
                "3.8.5"));
    notebookDto.setMetadata(metadata);

    String fileName = "testNotebook.ipynb";
    String directoryPath = "/path/to/notebooks";
    Path testPath = Paths.get(directoryPath, fileName);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock
          .when(() -> Files.createDirectories(testPath.getParent()))
          .thenReturn(testPath.getParent());
      filesMock
          .when(() -> Files.write(eq(testPath), any(byte[].class)))
          .thenThrow(new IOException("Simulated IO Exception"));

      String jsonString = objectMapper.writeValueAsString(notebookDto);
      assertThrows(
          RuntimeException.class, () -> fileStorageService.uploadNotebook(jsonString, fileName));
    }
  }

  @Test
  void testLoadNotebook_Success() {
    String fileName = "testNotebook.ipynb";
    String fullPath = "/path/to/notebooks/" + fileName;
    Path testPath = Paths.get(fullPath);

    String simulatedContent =
        "{\"nbformat\": 4, \"nbformat_minor\": 2, \"metadata\": {\"kernelspec\": {\"display_name\": \"Python 3\", \"language\": \"python\", \"name\": \"python3\"}, \"language_info\": {\"name\": \"python\"}}}";

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(testPath)).thenReturn(true);
      filesMock
          .when(() -> Files.readString(testPath, StandardCharsets.UTF_8))
          .thenReturn(simulatedContent);

      JupyterNotebookDTO result = fileStorageService.downloadNotebook(fullPath);

      assertNotNull(result);
      assertNotNull(result.getMetadata());

      assertEquals("Python 3", result.getMetadata().getKernelspec().getDisplayName());
      assertEquals("python", result.getMetadata().getKernelspec().getLanguage());
      assertEquals("python3", result.getMetadata().getKernelspec().getName());
      assertEquals("python", result.getMetadata().getLanguageInfo().getName());
    } catch (Exception e) {
      fail("Exception occurred: " + e.getMessage());
    }
  }

  @Test
  void testLoadNotebook_NotFound() {
    String fileName = "testNotebook.ipynb";
    String fullPath = "/path/to/notebooks/" + fileName;
    Path testPath = Paths.get(fullPath);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(testPath)).thenReturn(false);
      assertThrows(
          NotebookNotFoundException.class, () -> fileStorageService.downloadNotebook(fullPath));
    }
  }

  @Test
  void testLoadNotebook_ThrowsIOException() {
    String fileName = "testNotebook.ipynb";
    String fullPath = "/path/to/notebooks/" + fileName;
    Path testPath = Paths.get(fullPath);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(testPath)).thenReturn(true);
      filesMock
          .when(() -> Files.readString(testPath, StandardCharsets.UTF_8))
          .thenThrow(new IOException("Simulated IO Exception"));

      assertThrows(
          NotebookStorageException.class, () -> fileStorageService.downloadNotebook(fullPath));
    }
  }

  @Test
  void testDeleteNotebook_Success() {
    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", "/path/to/notebooks");

    String fileName = "testNotebook.ipynb";
    Path directoryPath = Paths.get("/path/to/notebooks");
    Path notebookPath = Paths.get(directoryPath.toString(), fileName);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(notebookPath)).thenReturn(true);
      filesMock.when(() -> Files.delete(notebookPath)).thenAnswer(invocation -> null);

      fileStorageService.deleteNotebook(fileName);
      filesMock.verify(() -> Files.delete(notebookPath), times(1));
    }
  }

  @Test
  void testDeleteNotebook_NotFound() {
    String fileName = "testNotebook.ipynb";
    String localStoragePath = "/path/to/notebooks";
    Path directoryPath = Paths.get(localStoragePath);
    Path notebookPath = Paths.get(directoryPath.toString(), fileName);

    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", localStoragePath);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(notebookPath)).thenReturn(false);
      assertThrows(
          NotebookNotFoundException.class, () -> fileStorageService.deleteNotebook(fileName));
      filesMock.verify(() -> Files.exists(notebookPath), times(1));
    }
  }

  @Test
  void testDeleteNotebook_ThrowsIOException() {
    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", "/path/to/notebooks");

    String fileName = "testNotebook.ipynb";
    Path directoryPath = Paths.get("/path/to/notebooks");
    Path notebookPath = Paths.get(directoryPath.toString(), fileName);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(notebookPath)).thenReturn(true);
      filesMock
          .when(() -> Files.delete(notebookPath))
          .thenThrow(new IOException("Simulated IO Exception"));

      assertThrows(
          NotebookStorageException.class, () -> fileStorageService.deleteNotebook(fileName));
    }
  }
}
