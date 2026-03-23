package org.jupytereverywhere.service;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    fileStorageService = new FileStorageService();
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

    byte[] jsonBytes = objectMapper.writeValueAsBytes(notebookDto);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock
          .when(() -> Files.createDirectories(testPath.getParent()))
          .thenReturn(testPath.getParent());
      filesMock.when(() -> Files.write(eq(testPath), any(byte[].class))).thenReturn(testPath);

      String resultPath = fileStorageService.uploadNotebook(jsonBytes, fileName);

      assertNotNull(resultPath);
      assertEquals(testPath.toString(), resultPath);

      filesMock.verify(() -> Files.write(eq(testPath), eq(jsonBytes)));
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

      byte[] jsonBytes = objectMapper.writeValueAsBytes(notebookDto);
      assertThrows(
          RuntimeException.class, () -> fileStorageService.uploadNotebook(jsonBytes, fileName));
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

      String result = fileStorageService.downloadNotebookAsJson(fullPath);

      assertNotNull(result);
      assertEquals(simulatedContent, result);
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
          NotebookNotFoundException.class,
          () -> fileStorageService.downloadNotebookAsJson(fullPath));
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
          NotebookStorageException.class,
          () -> fileStorageService.downloadNotebookAsJson(fullPath));
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

  @Test
  void testDeleteNotebooks_BatchSuccess() {
    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", "/path/to/notebooks");

    String file1 = "notebook1.ipynb";
    String file2 = "notebook2.ipynb";
    Path path1 = Paths.get("/path/to/notebooks", file1);
    Path path2 = Paths.get("/path/to/notebooks", file2);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(path1)).thenReturn(true);
      filesMock.when(() -> Files.exists(path2)).thenReturn(true);
      filesMock.when(() -> Files.delete(path1)).thenAnswer(invocation -> null);
      filesMock.when(() -> Files.delete(path2)).thenAnswer(invocation -> null);

      fileStorageService.deleteNotebooks(List.of(file1, file2));

      filesMock.verify(() -> Files.delete(path1), times(1));
      filesMock.verify(() -> Files.delete(path2), times(1));
    }
  }

  @Test
  void testDeleteNotebooks_BatchWithMissingFileSkips() {
    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", "/path/to/notebooks");

    String existingFile = "exists.ipynb";
    String missingFile = "missing.ipynb";
    Path existingPath = Paths.get("/path/to/notebooks", existingFile);
    Path missingPath = Paths.get("/path/to/notebooks", missingFile);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(existingPath)).thenReturn(true);
      filesMock.when(() -> Files.exists(missingPath)).thenReturn(false);
      filesMock.when(() -> Files.delete(existingPath)).thenAnswer(invocation -> null);

      fileStorageService.deleteNotebooks(List.of(existingFile, missingFile));

      filesMock.verify(() -> Files.delete(existingPath), times(1));
      filesMock.verify(() -> Files.delete(missingPath), never());
    }
  }

  @Test
  void testDeleteNotebooks_BatchWithIOErrorThrows() {
    ReflectionTestUtils.setField(fileStorageService, "localStoragePath", "/path/to/notebooks");

    String fileName = "error.ipynb";
    Path notebookPath = Paths.get("/path/to/notebooks", fileName);

    try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
      filesMock.when(() -> Files.exists(notebookPath)).thenReturn(true);
      filesMock
          .when(() -> Files.delete(notebookPath))
          .thenThrow(new IOException("Simulated IO Exception"));

      assertThrows(
          NotebookStorageException.class,
          () -> fileStorageService.deleteNotebooks(List.of(fileName)));
    }
  }
}
