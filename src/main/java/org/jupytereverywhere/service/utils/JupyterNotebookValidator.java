package org.jupytereverywhere.service.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;

@Log4j2
@Component
public class JupyterNotebookValidator {

  public static final String MESSAGE = "Message";

  @Value("${python.interpreter.path}")
  private String pythonInterpreterPath;

  @Value("${python.script.path}")
  private String pythonScriptPath;

  private final Function<List<String>, ProcessBuilder> processBuilderFactory;

  public JupyterNotebookValidator() {
    this.processBuilderFactory = ProcessBuilder::new;
  }

  public JupyterNotebookValidator(Function<List<String>, ProcessBuilder> processBuilderFactory) {
    this.processBuilderFactory = processBuilderFactory;
  }

  public boolean validateNotebook(String notebookJson) {
    try {
      Process process = startValidationProcess();
      writeNotebookToProcess(process, notebookJson);
      boolean isValid = readValidationResult(process);
      process.waitFor();
      return isValid;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      log.error(
          new StringMapMessage()
              .with(MESSAGE, "Thread was interrupted during notebook validation")
              .with("ExceptionType", e.getClass().getSimpleName())
              .with("ExceptionMessage", e.getMessage()),
          e
      );

      return false;
    } catch (IOException e) {

      log.error(
          new StringMapMessage()
              .with(MESSAGE, "IOException occurred during notebook validation")
              .with("ExceptionType", e.getClass().getSimpleName())
              .with("ExceptionMessage", e.getMessage()),
          e
      );

      return false;
    } catch (Exception e) {

      log.error(
          new StringMapMessage()
              .with(MESSAGE, "An error occurred during notebook validation")
              .with("ExceptionType", e.getClass().getSimpleName())
              .with("ExceptionMessage", e.getMessage()),
          e
      );

      return false;
    }
  }

  private Process startValidationProcess() throws IOException {
    log.info(
        new StringMapMessage()
            .with(MESSAGE, "Starting notebook validation process")
            .with("InterpreterPath", pythonInterpreterPath)
            .with("ScriptPath", pythonScriptPath)
    );

    String interpreter = pythonInterpreterPath;
    List<String> command = Arrays.asList(interpreter, pythonScriptPath);

    log.info(
        new StringMapMessage()
            .with(MESSAGE, "Executing command")
            .with("Command", String.join(" ", command))
    );

    ProcessBuilder processBuilder = processBuilderFactory.apply(command);
    processBuilder.redirectErrorStream(true);
    return processBuilder.start();
  }

  private void writeNotebookToProcess(Process process, String notebookJson) throws IOException {
    try (OutputStream stdin = process.getOutputStream()) {
      stdin.write(notebookJson.getBytes(StandardCharsets.UTF_8));
      stdin.flush();
    }
  }

  private boolean readValidationResult(Process process) throws IOException {
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
        String trimmedLine = line.trim();

        if ("valid".equals(trimmedLine)) {
          log.info(
              new StringMapMessage()
                  .with(MESSAGE, "Notebook is valid")
                  .with("Output", trimmedLine)
          );
          return true;
        } else if ("invalid".equals(trimmedLine)) {
          log.info(
              new StringMapMessage()
                  .with(MESSAGE, "Notebook is invalid")
                  .with("Output", trimmedLine)
          );
          return false;
        }
      }
    }

    log.error(
        new StringMapMessage()
            .with(MESSAGE, "Validation script did not produce expected output")
            .with("FullOutput", output.toString())
    );

    return false;
  }
}
