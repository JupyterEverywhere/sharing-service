package org.jupytereverywhere.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PythonJupyterNotebookValidatorTest {

  private PythonJupyterNotebookValidator notebookValidator;

  @Mock private ProcessBuilder processBuilder;

  @Mock private Process process;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    Function<List<String>, ProcessBuilder> processBuilderFactory =
        args -> {
          assertEquals("/usr/local/bin/python", args.get(0));
          assertEquals("/path/to/validate_notebook.py", args.get(1));
          return processBuilder;
        };

    notebookValidator = new PythonJupyterNotebookValidator(processBuilderFactory);

    ReflectionTestUtils.setField(
        notebookValidator, "pythonInterpreterPath", "/usr/local/bin/python");
    ReflectionTestUtils.setField(
        notebookValidator, "pythonScriptPath", "/path/to/validate_notebook.py");
  }

  @Test
  void testValidateNotebook_Valid() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    String simulatedOutput = "valid";
    InputStream inputStream =
        new ByteArrayInputStream(simulatedOutput.getBytes(StandardCharsets.UTF_8));
    when(process.getInputStream()).thenReturn(inputStream);

    OutputStream outputStream = mock(OutputStream.class);
    when(process.getOutputStream()).thenReturn(outputStream);

    when(process.waitFor()).thenReturn(0);

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertTrue(result);
  }

  @Test
  void testValidateNotebook_Invalid() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    String simulatedOutput = "invalid";
    InputStream inputStream =
        new ByteArrayInputStream(simulatedOutput.getBytes(StandardCharsets.UTF_8));
    when(process.getInputStream()).thenReturn(inputStream);

    OutputStream outputStream = mock(OutputStream.class);
    when(process.getOutputStream()).thenReturn(outputStream);

    when(process.waitFor()).thenReturn(1);

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }

  @Test
  void testValidateNotebook_WriteIOException() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    OutputStream outputStream = mock(OutputStream.class);
    when(process.getOutputStream()).thenReturn(outputStream);

    doThrow(new IOException("Test IOException"))
        .when(outputStream)
        .write(any(byte[].class), anyInt(), anyInt());

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }

  @Test
  void testValidateNotebook_ReadIOException() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    when(process.getOutputStream()).thenReturn(mock(OutputStream.class));

    InputStream inputStream = mock(InputStream.class);
    when(process.getInputStream()).thenReturn(inputStream);

    when(inputStream.read(any(byte[].class), anyInt(), anyInt()))
        .thenThrow(new IOException("Test IOException"));

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }

  @Test
  void testValidateNotebook_InterruptedException() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

    when(process.waitFor()).thenThrow(new InterruptedException("Test InterruptedException"));

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }

  @Test
  void testValidateNotebook_GeneralException() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    when(process.getOutputStream()).thenThrow(new RuntimeException("Test RuntimeException"));

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }

  @Test
  void testValidateNotebook_NoValidOrInvalidOutput() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    String simulatedOutput = "some other output";
    InputStream inputStream =
        new ByteArrayInputStream(simulatedOutput.getBytes(StandardCharsets.UTF_8));
    when(process.getInputStream()).thenReturn(inputStream);

    when(process.getOutputStream()).thenReturn(mock(OutputStream.class));

    when(process.waitFor()).thenReturn(0);

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }

  @Test
  void testValidateNotebook_ReadLineIOException() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    when(process.getOutputStream()).thenReturn(mock(OutputStream.class));

    InputStream inputStream = mock(InputStream.class);
    when(process.getInputStream()).thenReturn(inputStream);

    try (MockedConstruction<BufferedReader> mocked =
        Mockito.mockConstruction(
            BufferedReader.class,
            (mock, context) -> {
              when(mock.readLine()).thenThrow(new IOException("Test IOException"));
            })) {
      boolean result = notebookValidator.validateNotebook("notebook content");
      assertFalse(result);
    }
  }

  @Test
  void testValidateNotebook_NoOutput_NonZeroExitCode() throws Exception {
    when(processBuilder.redirectErrorStream(true)).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(process);

    InputStream inputStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    when(process.getInputStream()).thenReturn(inputStream);

    when(process.getOutputStream()).thenReturn(mock(OutputStream.class));

    when(process.waitFor()).thenReturn(1);

    boolean result = notebookValidator.validateNotebook("notebook content");
    assertFalse(result);
  }
}
