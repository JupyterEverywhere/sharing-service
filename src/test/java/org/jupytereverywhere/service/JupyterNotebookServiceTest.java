package org.jupytereverywhere.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;

import org.jupytereverywhere.dto.JupyterNotebookDTO;
import org.jupytereverywhere.dto.KernelspecDTO;
import org.jupytereverywhere.dto.LanguageInfoDTO;
import org.jupytereverywhere.dto.MetadataDTO;
import org.jupytereverywhere.exception.InvalidNotebookException;
import org.jupytereverywhere.exception.NotebookNotFoundException;
import org.jupytereverywhere.exception.NotebookStorageException;
import org.jupytereverywhere.exception.NotebookTooLargeException;
import org.jupytereverywhere.exception.UnauthorizedNotebookAccessException;
import org.jupytereverywhere.model.JupyterNotebookEntity;
import org.jupytereverywhere.model.request.JupyterNotebookRequest;
import org.jupytereverywhere.model.response.JupyterNotebookRetrieved;
import org.jupytereverywhere.model.response.JupyterNotebookSaved;
import org.jupytereverywhere.repository.JupyterNotebookRepository;
import org.jupytereverywhere.service.utils.JupyterNotebookValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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

  @Mock
  private JwtTokenService jwtTokenService;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UUID notebookId;
  private UUID sessionId;
  private String domain;
  private String readableId;
  private String token;

  @BeforeEach
  void setUp() {
    notebookId = UUID.randomUUID();
    sessionId = UUID.randomUUID();
    domain = "example.com";
    readableId = "readable-id";
    token = "jwt-token";

    // Set the maxNotebookSizeBytes field to 10MB (same as application.properties default)
    ReflectionTestUtils.setField(notebookService, "maxNotebookSizeBytes", 10485760L);
  }

  private JupyterNotebookDTO createSampleNotebookDTO() {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    MetadataDTO metadata = new MetadataDTO();
    notebookDto.setMetadata(metadata);
    notebookDto.setCells(new ArrayList<>());
    notebookDto.setNbformat(4);
    notebookDto.setNbformatMinor(2);
    return notebookDto;
  }

  private JupyterNotebookEntity createSampleNotebookEntity() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    entity.setId(notebookId);
    entity.setSessionId(sessionId);
    entity.setDomain(domain);
    entity.setReadableId(readableId);
    entity.setStorageUrl("storage-url");
    // Only set password if passwordEncoder is available
    if (passwordEncoder != null) {
      entity.setPassword(passwordEncoder.encode("password"));
    }
    return entity;
  }

  @Test
  void testGetNotebookContent_ByUUID_Success() {
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();
    JupyterNotebookDTO notebookDTO = createSampleNotebookDTO();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(storageService.downloadNotebook(notebookEntity.getStorageUrl())).thenReturn(notebookDTO);

    JupyterNotebookRetrieved result = notebookService.getNotebookContent(notebookId);

    assertNotNull(result);
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
    assertEquals(readableId, result.getReadableId());
    assertEquals(notebookDTO, result.getNotebookDTO());
  }

  @Test
  void testGetNotebookContent_ByUUID_NotFound() {
    when(notebookRepository.findById(notebookId)).thenReturn(Optional.empty());

    NotebookNotFoundException exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.getNotebookContent(notebookId);
    });

    assertEquals("Notebook not found", exception.getMessage());
  }

  @Test
  void testGetNotebookContent_ByUUID_StorageException() {
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(storageService.downloadNotebook(notebookEntity.getStorageUrl()))
        .thenThrow(new NotebookStorageException("Storage error"));

    NotebookStorageException exception = assertThrows(NotebookStorageException.class, () -> {
      notebookService.getNotebookContent(notebookId);
    });

    assertEquals("Error fetching notebook content from storage", exception.getMessage());
  }

  @Test
  void testGetNotebookContent_ByReadableId_Success() {
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();
    JupyterNotebookDTO notebookDTO = createSampleNotebookDTO();

    when(notebookRepository.findByReadableId(readableId)).thenReturn(Optional.of(notebookEntity));
    when(storageService.downloadNotebook(notebookEntity.getStorageUrl())).thenReturn(notebookDTO);

    JupyterNotebookRetrieved result = notebookService.getNotebookContent(readableId);

    assertNotNull(result);
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
    assertEquals(readableId, result.getReadableId());
    assertEquals(notebookDTO, result.getNotebookDTO());
  }

  @Test
  void testGetNotebookContent_ByReadableId_NotFound() {
    when(notebookRepository.findByReadableId(readableId)).thenReturn(Optional.empty());

    NotebookNotFoundException exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.getNotebookContent(readableId);
    });

    assertEquals("Notebook not found", exception.getMessage());
  }

  @Test
  void testUploadNotebook_Success() throws Exception {
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    notebookRequest.setNotebook(createSampleNotebookDTO());
    notebookRequest.setPassword("password");

    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("storage-url");
    when(notebookRepository.saveAndFlush(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      entity.setId(notebookId);
      entity.setReadableId(readableId);
      return entity;
    });
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    when(objectMapper.writeValueAsString(any())).thenReturn("serialized-notebook-json");
    doNothing().when(entityManager).refresh(any(JupyterNotebookEntity.class));

    JupyterNotebookSaved result = notebookService.uploadNotebook(notebookRequest, sessionId, domain);

    assertNotNull(result);
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
    assertNotNull(result.getReadableId());
  }

  @Test
  void testUploadNotebook_InvalidNotebook() {
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    notebookRequest.setNotebook(createSampleNotebookDTO());

    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(false);

    InvalidNotebookException exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.uploadNotebook(notebookRequest, sessionId, domain);
    });

    assertEquals("Notebook validation failed", exception.getMessage());
  }

  @Test
  void testUpdateNotebook_ByUUID_Success() throws Exception {
    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("storage-url");

    JupyterNotebookSaved result = notebookService.updateNotebook(notebookId, notebookDto, sessionId, token);

    assertNotNull(result);
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
  }

  @Test
  void testUpdateNotebook_ByUUID_SessionMismatch_NotebookIdMatches() throws Exception {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID differentSessionId = UUID.randomUUID();
    String token = "test-token";
    String domain = "test-domain";

    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();
    notebookEntity.setId(notebookId);
    notebookEntity.setSessionId(differentSessionId);
    notebookEntity.setDomain(domain);

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(jwtTokenService.extractNotebookIdFromToken(token)).thenReturn(notebookId.toString());
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("storage-url");

    JupyterNotebookSaved result = notebookService.updateNotebook(notebookId, notebookDto, sessionId, token);

    assertNotNull(result);
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
  }

  @Test
  void testUpdateNotebook_ByUUID_SessionMismatch_NotebookIdDoesNotMatch() {
    UUID notebookId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID differentSessionId = UUID.randomUUID();
    UUID differentNotebookId = UUID.randomUUID();
    String token = "test-token";

    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();
    notebookEntity.setId(notebookId);
    notebookEntity.setSessionId(differentSessionId);

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(jwtTokenService.extractNotebookIdFromToken(token)).thenReturn(differentNotebookId.toString());

    UnauthorizedNotebookAccessException exception = assertThrows(UnauthorizedNotebookAccessException.class, () -> {
      notebookService.updateNotebook(notebookId, notebookDto, sessionId, token);
    });

    assertEquals("You do not have permission to update this notebook", exception.getMessage());
  }


  @Test
  void testUpdateNotebook_ByUUID_NotFound() {
    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.empty());

    NotebookNotFoundException exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.updateNotebook(notebookId, notebookDto, sessionId, token);
    });

    assertEquals("Notebook not found with ID: " + notebookId, exception.getMessage());
  }

  @Test
  void testUpdateNotebook_ByReadableId_Success() throws Exception {
    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();
    notebookEntity.setSessionId(sessionId);

    when(notebookRepository.findByReadableId(readableId)).thenReturn(Optional.of(notebookEntity));
    when(notebookRepository.findById(notebookEntity.getId())).thenReturn(Optional.of(notebookEntity));

    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("storage-url");
    when(objectMapper.writeValueAsString(any())).thenReturn("serialized-notebook-json");

    JupyterNotebookSaved result = notebookService.updateNotebook(readableId, notebookDto, sessionId, token);

    assertNotNull(result);
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
  }

  @Test
  void testUpdateNotebook_ByReadableId_NotFound() {
    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();

    when(notebookRepository.findByReadableId(readableId)).thenReturn(Optional.empty());

    NotebookNotFoundException exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.updateNotebook(readableId, notebookDto, sessionId, token);
    });

    assertEquals("Notebook not found", exception.getMessage());
  }

  @Test
  void testValidateAndStoreNotebook_InvalidMetadata() {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(null);

    InvalidNotebookException exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.validateAndStoreNotebook(notebookDto, sessionId, domain, "password");
    });

    assertEquals("Invalid metadata format in notebook", exception.getMessage());
  }

  @Test
  void testValidateNotebookMetadata_InvalidMetadata() {
    JupyterNotebookDTO notebookDto = new JupyterNotebookDTO();
    notebookDto.setMetadata(null);

    InvalidNotebookException exception = assertThrows(InvalidNotebookException.class, () -> {
      notebookService.validateNotebookMetadata(notebookDto);
    });

    assertEquals("Invalid metadata format in notebook", exception.getMessage());
  }

  @Test
  void testStoreNotebook_Success() throws Exception {
    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();

    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("storage-url");

    String result = notebookService.storeNotebook(notebookDto, "filename.ipynb");

    assertEquals("storage-url", result);
  }

  @Test
  void testStoreNotebook_JsonProcessingException() throws Exception {
    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();

    doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(notebookDto);

    JsonProcessingException exception = assertThrows(JsonProcessingException.class, () -> {
      notebookService.storeNotebook(notebookDto, "filename.ipynb");
    });

    assertNotNull(exception);
  }

  @Test
  void testGetNotebookById_Success() {
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();

    when(notebookRepository.findNotebookById(notebookId)).thenReturn(Optional.of(notebookEntity));

    JupyterNotebookEntity result = notebookService.getNotebookById(notebookId);

    assertNotNull(result);
    assertEquals(notebookEntity, result);
  }

  @Test
  void testGetNotebookById_NotFound() {
    when(notebookRepository.findNotebookById(notebookId)).thenReturn(Optional.empty());

    NotebookNotFoundException exception = assertThrows(NotebookNotFoundException.class, () -> {
      notebookService.getNotebookById(notebookId);
    });

    assertEquals("Notebook not found with ID: " + notebookId, exception.getMessage());
  }

  @Test
  void testUploadNotebook_ReadableIdCannotBeSetExternally() throws Exception {
    // This test ensures that even if a malicious user tries to set a readable ID,
    // the service only relies on the database trigger to assign it

    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    notebookRequest.setNotebook(notebookDto);
    notebookRequest.setPassword("password");

    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("storage-url");
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");    when(notebookRepository.saveAndFlush(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      entity.setId(notebookId);
      entity.setReadableId("system-assigned-id"); // This would be set by the DB trigger
      return entity;
    });
    doNothing().when(entityManager).refresh(any(JupyterNotebookEntity.class));
    when(notebookRepository.save(any(JupyterNotebookEntity.class))).thenAnswer(invocation -> {
      JupyterNotebookEntity entity = invocation.getArgument(0);
      return entity;
    });

    JupyterNotebookSaved result = notebookService.uploadNotebook(notebookRequest, sessionId, domain);
    assertEquals("system-assigned-id", result.getReadableId());
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
  }

  @Test
  void testUpdateNotebook_ReadableIdRemainsUnchanged() throws Exception {
    // This test ensures that updating a notebook doesn't change the readable ID

    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();

    JupyterNotebookEntity existingEntity = createSampleNotebookEntity();
    existingEntity.setReadableId("original-readable-id");
    existingEntity.setStorageUrl("existing-storage-url");

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(existingEntity));
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("updated-storage-url");
    when(notebookRepository.save(any(JupyterNotebookEntity.class))).thenReturn(existingEntity);

    JupyterNotebookSaved result = notebookService.updateNotebook(notebookId, notebookDto, sessionId, token);
    assertEquals("original-readable-id", result.getReadableId());
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
  }

  @Test
  void testUpdateNotebookByReadableId_ReadableIdRemainsUnchanged() throws Exception {
    // This test ensures that updating a notebook by readable ID doesn't change the readable ID

    JupyterNotebookDTO notebookDto = createSampleNotebookDTO();

    JupyterNotebookEntity existingEntity = createSampleNotebookEntity();
    existingEntity.setReadableId("original-readable-id");
    existingEntity.setStorageUrl("existing-storage-url");

    when(notebookRepository.findByReadableId("original-readable-id")).thenReturn(Optional.of(existingEntity));
    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(existingEntity));
    when(jupyterNotebookValidator.validateNotebook(anyString())).thenReturn(true);
    when(storageService.uploadNotebook(anyString(), anyString())).thenReturn("updated-storage-url");
    when(notebookRepository.save(any(JupyterNotebookEntity.class))).thenReturn(existingEntity);

    JupyterNotebookSaved result = notebookService.updateNotebook("original-readable-id", notebookDto, sessionId, token);
    assertEquals("original-readable-id", result.getReadableId());
    assertEquals(notebookId, result.getId());
    assertEquals(domain, result.getDomain());
  }

  @Test
  void testValidateNotebookSize_ExceedsLimit() throws Exception {
    // Create a large notebook that exceeds 10MB
    StringBuilder largeContent = new StringBuilder();
    // Create content larger than 10MB (10485760 bytes)
    int targetSize = 11 * 1024 * 1024; // 11MB
    for (int i = 0; i < targetSize; i++) {
      largeContent.append("a");
    }

    String largeNotebookJson = largeContent.toString();

    NotebookTooLargeException exception = assertThrows(NotebookTooLargeException.class, () -> {
      notebookService.validateNotebookSize(largeNotebookJson, sessionId);
    });

    assertNotNull(exception);
    assertNotNull(exception.getMessage());
  }

  @Test
  void testValidateNotebookSize_WithinLimit() throws Exception {
    // Create a small notebook well within the 10MB limit
    String smallNotebookJson = "{\"nbformat\":4,\"cells\":[]}";

    // Should not throw any exception
    notebookService.validateNotebookSize(smallNotebookJson, sessionId);
  }

  @Test
  void testUploadNotebook_TooLarge() throws Exception {
    // Create a notebook that will exceed the size limit when serialized
    JupyterNotebookDTO largeNotebookDto = createSampleNotebookDTO();
    JupyterNotebookRequest notebookRequest = new JupyterNotebookRequest();
    notebookRequest.setNotebook(largeNotebookDto);

    // Mock the serialization to return a large JSON string
    StringBuilder largeContent = new StringBuilder();
    int targetSize = 11 * 1024 * 1024; // 11MB
    for (int i = 0; i < targetSize; i++) {
      largeContent.append("a");
    }
    String largeJson = largeContent.toString();

    when(objectMapper.writeValueAsString(any())).thenReturn(largeJson);

    NotebookTooLargeException exception = assertThrows(NotebookTooLargeException.class, () -> {
      notebookService.uploadNotebook(notebookRequest, sessionId, domain);
    });

    assertNotNull(exception);
    assertNotNull(exception.getMessage());
  }

  @Test
  void testUpdateNotebook_TooLarge() throws Exception {
    JupyterNotebookDTO largeNotebookDto = createSampleNotebookDTO();
    JupyterNotebookEntity notebookEntity = createSampleNotebookEntity();

    // Mock the serialization to return a large JSON string
    StringBuilder largeContent = new StringBuilder();
    int targetSize = 11 * 1024 * 1024; // 11MB
    for (int i = 0; i < targetSize; i++) {
      largeContent.append("a");
    }
    String largeJson = largeContent.toString();

    when(notebookRepository.findById(notebookId)).thenReturn(Optional.of(notebookEntity));
    when(objectMapper.writeValueAsString(any())).thenReturn(largeJson);

    NotebookTooLargeException exception = assertThrows(NotebookTooLargeException.class, () -> {
      notebookService.updateNotebook(notebookId, largeNotebookDto, sessionId, token);
    });

    assertNotNull(exception);
    assertNotNull(exception.getMessage());
  }

  @Test
  void testSetNotebookEntityMetadata_WithCompleteMetadata() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    MetadataDTO metadata = new MetadataDTO();

    LanguageInfoDTO languageInfo = new LanguageInfoDTO();
    languageInfo.setName("python");
    languageInfo.setVersion("3.9.7");
    languageInfo.setFileExtension(".py");
    metadata.setLanguageInfo(languageInfo);

    KernelspecDTO kernelspec = new KernelspecDTO();
    kernelspec.setName("python3");
    kernelspec.setDisplayName("Python 3");
    kernelspec.setLanguage("python");
    metadata.setKernelspec(kernelspec);

    ReflectionTestUtils.invokeMethod(notebookService, "setNotebookEntityMetadata", entity, metadata);

    assertEquals("python", entity.getLanguage());
    assertEquals("3.9.7", entity.getLanguageVersion());
    assertEquals(".py", entity.getFileExtension());
    assertEquals("python3", entity.getKernelName());
    assertEquals("Python 3", entity.getKernelDisplayName());
  }

  @Test
  void testSetNotebookEntityMetadata_WithNullLanguageInfo() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    MetadataDTO metadata = new MetadataDTO();

    // languageInfo is null - fields should remain NULL
    KernelspecDTO kernelspec = new KernelspecDTO();
    kernelspec.setName("python3");
    kernelspec.setDisplayName("Python 3");
    metadata.setKernelspec(kernelspec);

    ReflectionTestUtils.invokeMethod(notebookService, "setNotebookEntityMetadata", entity, metadata);

    assertNull(entity.getLanguage());
    assertNull(entity.getLanguageVersion());
    assertNull(entity.getFileExtension());
    assertEquals("python3", entity.getKernelName());
    assertEquals("Python 3", entity.getKernelDisplayName());
  }

  @Test
  void testSetNotebookEntityMetadata_WithNullKernelspec() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    MetadataDTO metadata = new MetadataDTO();

    // kernelspec is null - fields should remain NULL
    LanguageInfoDTO languageInfo = new LanguageInfoDTO();
    languageInfo.setName("r");
    languageInfo.setVersion("4.1.0");
    metadata.setLanguageInfo(languageInfo);

    ReflectionTestUtils.invokeMethod(notebookService, "setNotebookEntityMetadata", entity, metadata);

    assertEquals("r", entity.getLanguage());
    assertEquals("4.1.0", entity.getLanguageVersion());
    assertNull(entity.getKernelName());
    assertNull(entity.getKernelDisplayName());
  }

  @Test
  void testSetNotebookEntityMetadata_WithBothMetadataFieldsNull() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    MetadataDTO metadata = new MetadataDTO();
    // Both kernelspec and languageInfo are null

    ReflectionTestUtils.invokeMethod(notebookService, "setNotebookEntityMetadata", entity, metadata);

    // All metadata fields should remain NULL, which is valid per Jupyter spec
    assertNull(entity.getLanguage());
    assertNull(entity.getLanguageVersion());
    assertNull(entity.getFileExtension());
    assertNull(entity.getKernelName());
    assertNull(entity.getKernelDisplayName());
  }

  @Test
  void testSetNotebookEntityMetadata_WithNullFieldsInKernelspec() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    MetadataDTO metadata = new MetadataDTO();

    // kernelspec exists but individual fields are null
    KernelspecDTO kernelspec = new KernelspecDTO();
    // All fields remain null
    metadata.setKernelspec(kernelspec);

    LanguageInfoDTO languageInfo = new LanguageInfoDTO();
    languageInfo.setName("julia");
    metadata.setLanguageInfo(languageInfo);

    ReflectionTestUtils.invokeMethod(notebookService, "setNotebookEntityMetadata", entity, metadata);

    assertEquals("julia", entity.getLanguage());
    assertNull(entity.getKernelName());
    assertNull(entity.getKernelDisplayName());
  }

  @Test
  void testSetNotebookEntityMetadata_WithNullLanguageInfoName() {
    JupyterNotebookEntity entity = new JupyterNotebookEntity();
    MetadataDTO metadata = new MetadataDTO();

    // languageInfo exists but name is null
    LanguageInfoDTO languageInfo = new LanguageInfoDTO();
    languageInfo.setVersion("1.0.0");
    // name is null
    metadata.setLanguageInfo(languageInfo);

    KernelspecDTO kernelspec = new KernelspecDTO();
    kernelspec.setName("custom");
    kernelspec.setDisplayName("Custom Kernel");
    metadata.setKernelspec(kernelspec);

    ReflectionTestUtils.invokeMethod(notebookService, "setNotebookEntityMetadata", entity, metadata);

    // Language should be null since languageInfo.name is null
    assertNull(entity.getLanguage());
    assertEquals("1.0.0", entity.getLanguageVersion());
    assertEquals("custom", entity.getKernelName());
    assertEquals("Custom Kernel", entity.getKernelDisplayName());
  }
}
