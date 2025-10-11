package org.jupytereverywhere.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.StringMapMessage;

import org.jupytereverywhere.dto.JupyterNotebookDTO;
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
import org.jupytereverywhere.utils.DateUtils;

@Log4j2
@Service
public class JupyterNotebookService {

  public static final String MESSAGE_KEY = "Message";
  public static final String NOTEBOOK_ID_MESSAGE_KEY = "NotebookID";
  public static final String SESSION_ID_MESSAGE_KEY = "SessionId";
  public static final String DOMAIN_MESSAGE_KEY = "Domain";

  private static final String NOTEBOOK_NOT_FOUND_MESSAGE = "Notebook not found";
  public static final String NOTEBOOK_VALIDATION_FAILED_MESSAGE = "Notebook validation failed";

  private final StorageService storageService;
  private final JupyterNotebookValidator jupyterNotebookValidator;
  private final JupyterNotebookRepository notebookRepository;
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;

  private final JwtTokenService jwtTokenService;
  private final PasswordEncoder passwordEncoder;

  @Value("${notebook.max-size-bytes}")
  private long maxNotebookSizeBytes;

  public JupyterNotebookService(
      StorageService storageService,
      JupyterNotebookValidator jupyterNotebookValidator,
      JupyterNotebookRepository notebookRepository,
      EntityManager entityManager,
      ObjectMapper objectMapper, JwtTokenService jwtTokenService, PasswordEncoder passwordEncoder) {
    this.storageService = storageService;
    this.jupyterNotebookValidator = jupyterNotebookValidator;
    this.notebookRepository = notebookRepository;
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
    this.jwtTokenService = jwtTokenService;
    this.passwordEncoder = passwordEncoder;
  }

  public JupyterNotebookRetrieved getNotebookContent(UUID notebookId) {
    JupyterNotebookEntity notebookEntity = notebookRepository.findById(notebookId)
        .orElseThrow(() -> {
          log.error(new StringMapMessage()
              .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
              .with(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
          return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
        });

    JupyterNotebookDTO notebookContent = fetchNotebookContent(notebookEntity);

    return new JupyterNotebookRetrieved(
            notebookEntity.getId(),
            notebookEntity.getDomain(),
            notebookEntity.getReadableId(),
            notebookContent
    );
  }

  public JupyterNotebookDTO fetchNotebookContent(JupyterNotebookEntity notebookEntity) {
    try {
      return storageService.downloadNotebook(notebookEntity.getStorageUrl());
    } catch (Exception e) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Error fetching notebook content from storage")
          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString())
          .with("Error", e.getMessage()), e);
      throw new NotebookStorageException("Error fetching notebook content from storage", e);
    }
  }

  @Transactional
  public JupyterNotebookSaved uploadNotebook(JupyterNotebookRequest jupyterNotebookRequest, UUID sessionId, String domain)
      throws InvalidNotebookException {

    JupyterNotebookDTO notebookDto = jupyterNotebookRequest.getNotebook();
    String password = jupyterNotebookRequest.getPassword();

    log.info(new StringMapMessage()
        .with(MESSAGE_KEY, "Validating and storing notebook")
        .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
        .with(DOMAIN_MESSAGE_KEY, domain));

    try {

      JupyterNotebookEntity notebookEntity = validateAndStoreNotebook(notebookDto, sessionId, domain, password);

      return new JupyterNotebookSaved(notebookEntity.getId(), notebookEntity.getDomain(), notebookEntity.getReadableId());
    } catch (NotebookTooLargeException e) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Notebook size exceeds limit")
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
          .with(DOMAIN_MESSAGE_KEY, domain), e);
      throw e;
    } catch (InvalidNotebookException e) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, NOTEBOOK_VALIDATION_FAILED_MESSAGE)
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
          .with(DOMAIN_MESSAGE_KEY, domain), e);
      throw e;
    } catch (Exception e) {
      log.error(new StringMapMessage()
              .with(MESSAGE_KEY, "Error during notebook upload")
              .with(MESSAGE_KEY, "Error during notebook upload")
              .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
              .with(DOMAIN_MESSAGE_KEY, domain)
              .with("Error",e.getMessage()));
      throw new RuntimeException("Error uploading notebook", e);
    }
  }

  public JupyterNotebookEntity validateAndStoreNotebook(JupyterNotebookDTO notebookDto,
      UUID sessionId, String domain, String password)
      throws InvalidNotebookException, JsonProcessingException {

    validateNotebookMetadata(notebookDto);

    String notebookJsonString = objectMapper.writeValueAsString(notebookDto);

    validateNotebookSize(notebookJsonString, sessionId);

    if (!jupyterNotebookValidator.validateNotebook(notebookJsonString)) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, NOTEBOOK_VALIDATION_FAILED_MESSAGE)
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString()));
      throw new InvalidNotebookException(NOTEBOOK_VALIDATION_FAILED_MESSAGE);
    }

    JupyterNotebookEntity notebookEntity = saveNotebookMetadata(sessionId,
        notebookDto.getMetadata(), domain, password);

    String fileName = notebookEntity.getId().toString() + ".ipynb";

    String storageUrl = storeNotebook(notebookDto, fileName);

    notebookEntity.setStorageUrl(storageUrl);
    notebookRepository.save(notebookEntity);

    return notebookEntity;
  }

  public JupyterNotebookSaved updateNotebook(UUID notebookId, JupyterNotebookDTO notebookDto, UUID sessionId, String token)
      throws UnauthorizedNotebookAccessException, InvalidNotebookException, JsonProcessingException {

    JupyterNotebookEntity storedNotebook = notebookRepository.findById(notebookId)
        .orElseThrow(() -> new NotebookNotFoundException("Notebook not found with ID: " + notebookId));

    Map<String, String> commonLogDetails = Map.of(
        NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString(),
        "StoredSessionId", storedNotebook.getSessionId().toString(),
        "ProvidedSessionId", sessionId.toString()
    );

    if (sessionId.equals(storedNotebook.getSessionId())) {
      logInfo("Session IDs match", commonLogDetails);
    } else {
      logInfo("Session ID mismatch", commonLogDetails);

      String notebookIdFromToken = jwtTokenService.extractNotebookIdFromToken(token);

      if (notebookIdFromToken == null) {
        logInfo("Notebook ID missing in token", Map.of(
            NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()
        ));
        throw new UnauthorizedNotebookAccessException("You do not have permission to update this notebook");
      }

      if (!notebookId.equals(UUID.fromString(notebookIdFromToken))) {
        logInfo("Unauthorized notebook update attempt", Map.of(
            NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString(),
            "NotebookIdFromToken", notebookIdFromToken
        ));
        throw new UnauthorizedNotebookAccessException("You do not have permission to update this notebook");
      }

      logInfo("Notebook ID validation succeeded", Map.of(NOTEBOOK_ID_MESSAGE_KEY, notebookId.toString()));
    }

    validateNotebookMetadata(notebookDto);

    String notebookJsonString = objectMapper.writeValueAsString(notebookDto);

    validateNotebookSize(notebookJsonString, sessionId);

    if (!jupyterNotebookValidator.validateNotebook(notebookJsonString)) {
      throw new InvalidNotebookException(NOTEBOOK_VALIDATION_FAILED_MESSAGE);
    }

    String fileName = storedNotebook.getId().toString() + ".ipynb";
    storeNotebook(notebookDto, fileName);

    updateNotebookMetadata(storedNotebook, notebookDto, sessionId);

    return new JupyterNotebookSaved(
        storedNotebook.getId(), storedNotebook.getDomain(), storedNotebook.getReadableId());
  }

  void validateNotebookSize(String notebookJsonString, UUID sessionId) {
    long notebookSizeBytes = notebookJsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

    if (notebookSizeBytes > maxNotebookSizeBytes) {
      long maxSizeMB = maxNotebookSizeBytes / (1024 * 1024);
      String errorMessage = String.format("Notebook size (%d bytes) exceeds maximum allowed size of %d MB",
          notebookSizeBytes, maxSizeMB);

      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, errorMessage)
          .with(SESSION_ID_MESSAGE_KEY, sessionId.toString())
          .with("NotebookSizeBytes", String.valueOf(notebookSizeBytes))
          .with("MaxSizeBytes", String.valueOf(maxNotebookSizeBytes)));

      throw new NotebookTooLargeException(errorMessage);
    }
  }

  void validateNotebookMetadata(JupyterNotebookDTO notebookDto)
      throws InvalidNotebookException {

    MetadataDTO metadata = notebookDto.getMetadata();
    if (metadata == null) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Invalid metadata format in notebook"));
      throw new InvalidNotebookException("Invalid metadata format in notebook");
    }

    // Validate language_info if present - name is required in notebook format 4.5
    if (metadata.getLanguageInfo() != null) {
      if (metadata.getLanguageInfo().getName() == null || metadata.getLanguageInfo().getName().trim().isEmpty()) {
        log.error(new StringMapMessage()
            .with(MESSAGE_KEY, "language_info.name is required in notebook format 4.5"));
        throw new InvalidNotebookException("language_info.name is required in notebook format 4.5");
      }
    }

    notebookDto.setMetadata(metadata);
  }

  String storeNotebook(JupyterNotebookDTO notebookDto, String fileName) throws JsonProcessingException {
    String notebookJsonString = objectMapper.writeValueAsString(notebookDto);
    return storageService.uploadNotebook(notebookJsonString, fileName);
  }

  JupyterNotebookEntity saveNotebookMetadata(UUID sessionId, MetadataDTO metadata, String domain, String password) {

    JupyterNotebookEntity notebookEntity = new JupyterNotebookEntity();
    notebookEntity.setSessionId(sessionId);
    notebookEntity.setDomain(domain);
    notebookEntity.setStorageUrl("");

    if (password != null && !password.isEmpty()) {
      notebookEntity.setPassword(passwordEncoder.encode(password));
    }

    setNotebookEntityMetadata(notebookEntity, metadata);

    Timestamp createdAt = DateUtils.utcDateToTimestamp(Date.from(Instant.now()));

    notebookEntity.setCreatedAt(createdAt);

    JupyterNotebookEntity savedNotebook = notebookRepository.saveAndFlush(notebookEntity);
    entityManager.refresh(savedNotebook);

    log.info(new StringMapMessage()
        .with(MESSAGE_KEY, "Notebook metadata saved in database")
        .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString())
        .with("CreatedAt", createdAt.toString()));

    return savedNotebook;
  }

  void updateNotebookMetadata(JupyterNotebookEntity notebookEntity, JupyterNotebookDTO notebookDto,
      UUID sessionId) {

    MetadataDTO metadata = notebookDto.getMetadata();
    if (metadata == null) {
      log.error(new StringMapMessage()
          .with(MESSAGE_KEY, "Metadata is missing in the notebook DTO")
          .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString()));
      throw new InvalidNotebookException("Metadata is missing");
    }

    notebookEntity.setSessionId(sessionId);
    setNotebookEntityMetadata(notebookEntity, metadata);

    notebookRepository.save(notebookEntity);

    log.info(new StringMapMessage()
        .with(MESSAGE_KEY, "Notebook metadata updated in database")
        .with(NOTEBOOK_ID_MESSAGE_KEY, notebookEntity.getId().toString()));
  }

  private void setNotebookEntityMetadata(JupyterNotebookEntity notebookEntity, MetadataDTO metadata) {
    if (metadata.getKernelspec() != null) {
      notebookEntity.setKernelName(metadata.getKernelspec().getName());
      notebookEntity.setKernelDisplayName(metadata.getKernelspec().getDisplayName());
    }

    if (metadata.getLanguageInfo() != null) {
      // Only 'name' is required in notebook format 4.5
      notebookEntity.setLanguage(metadata.getLanguageInfo().getName());

      // Optional fields - only set if present
      if (metadata.getLanguageInfo().getVersion() != null) {
        notebookEntity.setLanguageVersion(metadata.getLanguageInfo().getVersion());
      }
      if (metadata.getLanguageInfo().getFileExtension() != null) {
        notebookEntity.setFileExtension(metadata.getLanguageInfo().getFileExtension());
      }
    }
  }

  public JupyterNotebookRetrieved getNotebookContent(String readableId) {
    JupyterNotebookEntity notebookEntity = notebookRepository.findByReadableId(readableId)
            .orElseThrow(() -> {
              log.error(new StringMapMessage()
                      .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                      .with("ReadableId", readableId)
              );
              return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
            });

    JupyterNotebookDTO notebookContent = fetchNotebookContent(notebookEntity);

    return new JupyterNotebookRetrieved(
            notebookEntity.getId(),
            notebookEntity.getDomain(),
            notebookEntity.getReadableId(),
            notebookContent
    );
  }

  public JupyterNotebookSaved updateNotebook(String readableId, JupyterNotebookDTO notebookDto, UUID sessionId, String token) throws JsonProcessingException {
    JupyterNotebookEntity notebookEntity = notebookRepository.findByReadableId(readableId)
            .orElseThrow(() -> {
              log.error(new StringMapMessage()
                      .with(MESSAGE_KEY, NOTEBOOK_NOT_FOUND_MESSAGE)
                      .with("ReadableId", readableId)
              );
              return new NotebookNotFoundException(NOTEBOOK_NOT_FOUND_MESSAGE);
            });

    updateNotebook(notebookEntity.getId(), notebookDto, sessionId, token);

    return new JupyterNotebookSaved(notebookEntity.getId(), notebookEntity.getDomain(), notebookEntity.getReadableId());
  }

  public JupyterNotebookEntity getNotebookById(UUID notebookId) {
    return notebookRepository.findNotebookById(notebookId)
        .orElseThrow(() -> new NotebookNotFoundException("Notebook not found with ID: " + notebookId));
  }

  private void logInfo(String message, Map<String, String> details) {
    StringMapMessage logMessage = new StringMapMessage().with(MESSAGE_KEY, message);
    details.forEach(logMessage::with);
    log.info(logMessage);
  }
}
