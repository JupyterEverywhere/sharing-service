package org.jupytereverywhere.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;

import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.exception.NotebookStorageException;

@Log4j2
@Service("fileStorageService")
public class FileStorageService implements StorageService {

  public static final String MESSAGE = "Message";
  public static final String NOTEBOOK_NAME = "NotebookName";
  public static final String NOTEBOOK_PATH = "NotebookPath";
  public static final String ERROR = "Error";

  @Value("${storage.path.local}")
  private String localStoragePath;

  private final ObjectMapper objectMapper = new ObjectMapper();

  void ensureDirectoryExists(Path directoryPath) throws IOException {
    if (!Files.exists(directoryPath)) {
      Files.createDirectories(directoryPath);
      StringMapMessage logMessage = new StringMapMessage()
          .with(MESSAGE, "Directory created")
          .with("DirectoryPath", directoryPath.toString());
      log.info(logMessage);
    }
  }

  @Override
  public String uploadNotebook(String notebookJsonString, String notebookName) {
    Path notebookPath = null;
    try {
      Path directoryPath = Paths.get(localStoragePath);
      notebookPath = Paths.get(directoryPath.toString(), notebookName);

      ensureDirectoryExists(notebookPath.getParent());

      Files.write(notebookPath, notebookJsonString.getBytes(StandardCharsets.UTF_8));

      StringMapMessage logMessage = new StringMapMessage()
          .with(MESSAGE, "Notebook saved")
          .with(NOTEBOOK_NAME, notebookName)
          .with(NOTEBOOK_PATH, notebookPath.toString());
      log.info(logMessage);

      return notebookPath.toString();
    } catch (IOException e) {

      StringMapMessage errorLog = new StringMapMessage()
          .with(MESSAGE, "Error saving notebook")
          .with(NOTEBOOK_NAME, notebookName)
          .with(NOTEBOOK_PATH, notebookPath.toString())
          .with(ERROR, e.getMessage());
      log.error(errorLog, e);
      throw new NotebookStorageException("Error saving notebook: " + notebookName + " at path: " + notebookPath, e);
    }
  }

  @Override
  public JupyterNotebookDTO downloadNotebook(String fullPath) {
    try {
      Path path = Paths.get(fullPath);

      StringMapMessage loadLog = new StringMapMessage()
          .with(MESSAGE, "Loading notebook")
          .with(NOTEBOOK_PATH, fullPath);
      log.info(loadLog);

      if (!Files.exists(path)) {
        StringMapMessage notFoundLog = new StringMapMessage()
            .with(MESSAGE, "Notebook not found")
            .with(NOTEBOOK_PATH, fullPath);
        log.error(notFoundLog);
        throw new NotebookNotFoundException("Notebook not found: " + fullPath);
      }

      String notebookContent = Files.readString(path, StandardCharsets.UTF_8);

      return objectMapper.readValue(notebookContent, JupyterNotebookDTO.class);
    } catch (IOException e) {
      StringMapMessage errorLog = new StringMapMessage()
          .with(MESSAGE, "Error loading notebook from local storage")
          .with(NOTEBOOK_PATH, fullPath)
          .with(ERROR, e.getMessage());
      log.error(errorLog, e);
      throw new NotebookStorageException("Error loading notebook from local storage: " + fullPath, e);
    }
  }

  @Override
  public void deleteNotebook(String fileName) {
    Path notebookPath = null;
    try {
      Path directoryPath = Paths.get(localStoragePath);
      notebookPath = Paths.get(directoryPath.toString(), fileName);

      if (Files.exists(notebookPath)) {
        Files.delete(notebookPath);

        StringMapMessage deleteLog = new StringMapMessage()
            .with(MESSAGE, "Notebook deleted")
            .with(NOTEBOOK_NAME, fileName)
            .with(NOTEBOOK_PATH, notebookPath.toString());
        log.info(deleteLog);
      } else {

        StringMapMessage warnLog = new StringMapMessage()
            .with(MESSAGE, "Notebook not found for deletion")
            .with(NOTEBOOK_PATH, notebookPath.toString());
        log.warn(warnLog);
        throw new NotebookNotFoundException("Notebook not found: " + notebookPath.toString());
      }
    } catch (IOException e) {
      StringMapMessage errorLog = new StringMapMessage()
          .with(MESSAGE, "Error deleting notebook")
          .with(NOTEBOOK_NAME, fileName)
          .with(NOTEBOOK_PATH, notebookPath.toString())
          .with(ERROR, e.getMessage());
      log.error(errorLog, e);
      throw new NotebookStorageException("Error deleting notebook: " + fileName + " at path: " + notebookPath, e);
    }
  }
}
