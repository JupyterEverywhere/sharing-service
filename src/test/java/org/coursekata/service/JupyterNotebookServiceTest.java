package org.coursekata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.coursekata.dto.CodemirrorModeDTO;
import org.coursekata.dto.JupyterNotebookDTO;
import org.coursekata.dto.KernelspecDTO;
import org.coursekata.dto.LanguageInfoDTO;
import org.coursekata.dto.MetadataDTO;
import org.coursekata.exception.InvalidNotebookException;
import org.coursekata.exception.NotebookNotFoundException;
import org.coursekata.exception.NotebookSerializationException;
import org.coursekata.exception.NotebookStorageException;
import org.coursekata.exception.SessionMismatchException;
import org.coursekata.model.JupyterNotebookEntity;
import org.coursekata.model.response.JupyterNotebookRetrieved;
import org.coursekata.repository.JupyterNotebookRepository;
import org.coursekata.service.utils.JupyterNotebookValidator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JupyterNotebookServiceTest {

  @InjectMocks
  private JupyterNotebookService notebookService;

  @Mock
  private StorageService storageService;

  @Mock
  private JupyterNotebookValidator jupyterNotebookValidator;

  @Mock
  private JupyterNotebookRepository notebookRepository;

  @Mock
  private EntityManager entityManager;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testGetNotebookContent_NotebookFound() {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setId(notebookId);
    notebookEntity.setStorageUrl("test_notebook.ipynb");

    JupyterNotebookDTO notebookDTO = new JupyterNotebookDTO();
    notebookDTO.setMetadata(new MetadataDTO());

    JupyterNotebookRetrieved notebookRetrieved = new JupyterNotebookRetrieved(
            notebookEntity.getId(),
            notebookEntity.getDomain(),
            notebookEntity.getReadableId(),
            notebookDTO
    );

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));


    when(storageService.downloadNotebook("test_notebook.ipynb")).thenReturn(notebookDTO);

    JupyterNotebookRetrieved result = notebookService.getNotebookContent(notebookId);

    assertNotNull(result);
    assertEquals(notebookRetrieved, result);
    verify(storageService).downloadNotebook("test_notebook.ipynb");
  }

  @Test
  void testGetNotebookContent_NotebookNotFound() {
    UUID notebookId = UUID.randomUUID();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.empty());

    Exception exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.getNotebookContent(notebookId);
    });

    assertEquals("Notebook not found", exception.getMessage());
    verify(notebookRepository).findById(notebookId);
    verifyNoInteractions(storageService);
  }

  @Test
  void testGetNotebookContent_Exception() {
    UUID notebookId = UUID.randomUUID();
    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setId(notebookId);
    notebookEntity.setStorageUrl("test_notebook.ipynb");

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(storageService.downloadNotebook("test_notebook.ipynb"))
        .thenThrow(new NotebookStorageException("Simulated storage exception"));

    NotebookStorageException exception = assertThrows(NotebookStorageException.class, () -> {
      notebookService.getNotebookContent(notebookId);
    });

    assertEquals("Error fetching notebook content from storage", exception.getMessage());
    verify(notebookRepository).findById(notebookId);
    verify(storageService).downloadNotebook("test_notebook.ipynb");
  }

  @Test
  void testValidateAndStoreNotebook_ValidNotebook() throws Exception {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(new MetadataDTO());
    notebookDto.setCells(new ArrayList<>());
    notebookDto.setNbformat(4);
    notebookDto.setNbformatMinor(2);
    UUID sessionId = UUID.randomUUID();
    String domain = "example.com";

    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("notebook.ipynb");
    when(notebookRepository.saveAndFlush(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      entity.setId(UUID.randomUUID());
      return entity;
    });

    notebookService.validateAndStoreNotebook(notebookDto, sessionId, domain);

    verify(storageService).uploadNotebook(anyString(), anyString());
    verify(notebookRepository).saveAndFlush(any(JupyterNotebookEntity.class));
  }

  @Test
  void testValidateAndStoreNotebook_InvalidMetadataFormat() {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(null);
    UUID sessionId = UUID.randomUUID();
    String domain = "example.com";

    Exception exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.validateAndStoreNotebook(notebookDto, sessionId, domain);
    });

    assertEquals("Invalid metadata format in notebook", exception.getMessage());
    verifyNoInteractions(jupyterNotebookValidator, storageService, notebookRepository);
  }

  @Test
  void testValidateAndStoreNotebook_ValidationFails() {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(new MetadataDTO());
    notebookDto.setCells(new ArrayList<>());
    notebookDto.setNbformat(4);
    notebookDto.setNbformatMinor(2);
    UUID sessionId = UUID.randomUUID();
    String domain = "example.com";

    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(false);

    Exception exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.validateAndStoreNotebook(notebookDto, sessionId, domain);
    });

    assertEquals("Notebook validation failed", exception.getMessage());
    verify(jupyterNotebookValidator).validateNotebook(anyString());
    verifyNoInteractions(storageService, notebookRepository);
  }

  @Test
  void testValidateAndStoreNotebook_KernelSpecAndLanguageInfoNull() throws Exception {
    UUID sessionId = UUID.randomUUID();
    String domain = "example.com";
    MetadataDTO metadata = new MetadataDTO(null, null);
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(metadata);

    final JupyterNotebookEntity[] savedEntityFirst = new JupyterNotebookEntity[1];
    final JupyterNotebookEntity[] savedEntitySecond = new JupyterNotebookEntity[1];

    when(notebookRepository.saveAndFlush(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      entity.setId(UUID.randomUUID());
      savedEntityFirst[0] = copyNotebookEntity(entity);
      return entity;
    });

    when(notebookRepository.save(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      savedEntitySecond[0] = copyNotebookEntity(entity);
      return entity;
    });

    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("notebook.ipynb");
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);

    notebookService.validateAndStoreNotebook(notebookDto, sessionId, domain);

    assertEquals(sessionId, savedEntityFirst[0].getSessionId());
    assertEquals(domain, savedEntityFirst[0].getDomain());
    assertNotNull(savedEntityFirst[0].getCreatedAt());
    assertNotNull(savedEntityFirst[0].getStorageUrl());
    assertNotNull(savedEntityFirst[0].getId());

    assertEquals(sessionId, savedEntitySecond[0].getSessionId());
    assertEquals(domain, savedEntitySecond[0].getDomain());
    assertNotNull(savedEntitySecond[0].getCreatedAt());
    assertEquals("notebook.ipynb", savedEntitySecond[0].getStorageUrl());
    assertNotNull(savedEntitySecond[0].getId());

    assertNull(savedEntitySecond[0].getKernelName());
    assertNull(savedEntitySecond[0].getKernelDisplayName());
    assertNull(savedEntitySecond[0].getLanguage());
    assertNull(savedEntitySecond[0].getLanguageVersion());
    assertNull(savedEntitySecond[0].getFileExtension());
  }

  @Test
  void testSaveNotebookMetadata_KernelSpecAndLanguageInfoNull() {
    UUID sessionId = UUID.randomUUID();
    String domain = "example.com";
    MetadataDTO metadata = new MetadataDTO(null, null);

    when(notebookRepository.saveAndFlush(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      entity.setId(UUID.randomUUID());
      return entity;
    });

    notebookService.saveNotebookMetadata(sessionId, metadata, domain);

    ArgumentCaptor<JupyterNotebookEntity> captor = ArgumentCaptor.forClass(JupyterNotebookEntity.class);
    verify(notebookRepository).saveAndFlush(captor.capture());

    JupyterNotebookEntity savedEntity = captor.getValue();
    assertEquals(sessionId, savedEntity.getSessionId());
    assertNotNull(savedEntity.getStorageUrl());
    assertEquals(domain, savedEntity.getDomain());
    assertNotNull(savedEntity.getCreatedAt());

    assertNull(savedEntity.getKernelName());
    assertNull(savedEntity.getKernelDisplayName());
    assertNull(savedEntity.getLanguage());
    assertNull(savedEntity.getLanguageVersion());
    assertNull(savedEntity.getFileExtension());

    assertNotNull(savedEntity.getId());
  }

  @Test
  void testUpdateNotebook_SuccessfulUpdate() throws Exception {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    JupyterNotebookEntity existingNotebook = new JupyterNotebookEntity();
    existingNotebook.setId(notebookId);
    existingNotebook.setSessionId(sessionId);
    existingNotebook.setStorageUrl("existing_notebook.ipynb");

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(existingNotebook));
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("updated_notebook.ipynb");

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(new MetadataDTO());
    notebookDto.setCells(new ArrayList<>());
    notebookDto.setNbformat(4);
    notebookDto.setNbformatMinor(2);

    notebookService.updateNotebook(notebookId, notebookDto, sessionId);

    verify(notebookRepository).findById(notebookId);
    verify(jupyterNotebookValidator).validateNotebook(anyString());
    verify(storageService).uploadNotebook(anyString(), anyString());
    verify(notebookRepository).save(any(JupyterNotebookEntity.class));
  }

  @Test
  void testUpdateNotebook_NotebookNotFound() {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.empty());

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();

    Exception exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.updateNotebook(notebookId, notebookDto, sessionId);
    });

    assertEquals("Notebook not found", exception.getMessage());
    verify(notebookRepository).findById(notebookId);
    verifyNoMoreInteractions(jupyterNotebookValidator, storageService, notebookRepository);
  }

  @Test
  void testUpdateNotebook_SessionMismatch() {
    UUID notebookId = UUID.randomUUID();
    UUID storedSessionId = UUID.randomUUID();
    UUID providedSessionId = UUID.randomUUID();

    JupyterNotebookEntity existingNotebook = new JupyterNotebookEntity();
    existingNotebook.setId(notebookId);
    existingNotebook.setSessionId(storedSessionId);

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(existingNotebook));

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();

    Exception exception = assertThrows(SessionMismatchException.class, () -> {
      notebookService.updateNotebook(notebookId, notebookDto, providedSessionId);
    });

    assertEquals("Session ID mismatch", exception.getMessage());
    verify(notebookRepository).findById(notebookId);
    verifyNoMoreInteractions(jupyterNotebookValidator, storageService, notebookRepository);
  }

  @Test
  void testUpdateNotebook_InvalidNotebook() {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    JupyterNotebookEntity existingNotebook = new JupyterNotebookEntity();
    existingNotebook.setId(notebookId);
    existingNotebook.setSessionId(sessionId);

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(existingNotebook));
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(false);

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(new MetadataDTO());

    Exception exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.updateNotebook(notebookId, notebookDto, sessionId);
    });

    assertEquals("Notebook validation failed", exception.getMessage());
    verify(notebookRepository).findById(notebookId);
    verify(jupyterNotebookValidator).validateNotebook(anyString());
    verifyNoMoreInteractions(storageService, notebookRepository);
  }

  @Test
  void testUpdateNotebookMetadata_SuccessfulUpdate() {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    KernelspecDTO kernelSpec = new KernelspecDTO("Python 3", "python", "python3");
    CodemirrorModeDTO codeMirrorMode = new CodemirrorModeDTO("python", 3);
    LanguageInfoDTO languageInfo = new LanguageInfoDTO(
        codeMirrorMode,
        ".py",
        "text/x-python",
        "python",
        "python3",
        "3.8"
    );

    MetadataDTO metadata = new MetadataDTO(kernelSpec, languageInfo);

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(metadata);

    JupyterNotebookEntity existingNotebook = new JupyterNotebookEntity();
    existingNotebook.setId(notebookId);
    existingNotebook.setSessionId(sessionId);

    when(notebookRepository.save(any(JupyterNotebookEntity.class))).thenReturn(existingNotebook);

    notebookService.updateNotebookMetadata(existingNotebook, notebookDto, sessionId);

    verify(notebookRepository).save(any(JupyterNotebookEntity.class));
  }

  @Test
  void testUpdateNotebookMetadata_MetadataMissing() {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(null);

    JupyterNotebookEntity existingNotebook = new JupyterNotebookEntity();
    existingNotebook.setId(notebookId);
    existingNotebook.setSessionId(sessionId);

    Exception exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.updateNotebookMetadata(existingNotebook, notebookDto, sessionId);
    });

    assertEquals("Metadata is missing", exception.getMessage());
    verifyNoMoreInteractions(notebookRepository);
  }

  @Test
  void testFetchNotebookContent_SuccessfulFetch() {
    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setId(UUID.randomUUID());
    notebookEntity.setStorageUrl("test_notebook.ipynb");

    JupyterNotebookDTO notebookDTO = new JupyterNotebookDTO();
    notebookDTO.setMetadata(new MetadataDTO());

    when(storageService.downloadNotebook("test_notebook.ipynb")).thenReturn(notebookDTO);

    JupyterNotebookDTO result = notebookService.fetchNotebookContent(notebookEntity);

    assertNotNull(result);
    assertEquals(notebookDTO, result);
    verify(storageService).downloadNotebook("test_notebook.ipynb");
  }

  @Disabled("Content is returned as object, not stringified")
  @Test
  void testFetchNotebookContent_SerializationException() throws Exception {
    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setId(UUID.randomUUID());
    notebookEntity.setStorageUrl("test_notebook.ipynb");

    JupyterNotebookDTO notebookDTO = new JupyterNotebookDTO();
    notebookDTO.setMetadata(new MetadataDTO());

    when(storageService.downloadNotebook("test_notebook.ipynb")).thenReturn(notebookDTO);
    doThrow(new JsonProcessingException("Simulated serialization error") {})
        .when(objectMapper).writeValueAsString(notebookDTO);

    Exception exception = assertThrows(NotebookSerializationException.class, () -> {
      notebookService.fetchNotebookContent(notebookEntity);
    });

    assertEquals("Error serializing notebook content", exception.getMessage());
    verify(storageService).downloadNotebook("test_notebook.ipynb");
    verify(objectMapper).writeValueAsString(notebookDTO);
  }

  @Test
  void testFetchNotebookContent_StorageException() {
    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setId(UUID.randomUUID());
    notebookEntity.setStorageUrl("test_notebook.ipynb");

    when(storageService.downloadNotebook("test_notebook.ipynb"))
        .thenThrow(new NotebookStorageException("Simulated storage exception"));

    Exception exception = assertThrows(NotebookStorageException.class, () -> {
      notebookService.fetchNotebookContent(notebookEntity);
    });

    assertEquals("Error fetching notebook content from storage", exception.getMessage());
    verify(storageService).downloadNotebook("test_notebook.ipynb");
    verifyNoMoreInteractions(objectMapper);
  }

  @Test
  void testStoreNotebook_SuccessfulStore() throws Exception {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(new MetadataDTO());
    String filename = "test";

    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("stored_notebook.ipynb");

    String result = notebookService.storeNotebook(notebookDto, filename);

    assertEquals("stored_notebook.ipynb", result);
    verify(storageService).uploadNotebook(anyString(), anyString());
  }

  private JupyterNotebookEntity copyNotebookEntity(JupyterNotebookEntity original) {
    JupyterNotebookEntity copy = new JupyterNotebookEntity();
    copy.setId(original.getId());
    copy.setSessionId(original.getSessionId());
    copy.setDomain(original.getDomain());
    copy.setStorageUrl(original.getStorageUrl());
    copy.setCreatedAt(original.getCreatedAt());
    copy.setKernelName(original.getKernelName());
    copy.setKernelDisplayName(original.getKernelDisplayName());
    copy.setLanguage(original.getLanguage());
    copy.setLanguageVersion(original.getLanguageVersion());
    copy.setFileExtension(original.getFileExtension());
    return copy;
  }
}
